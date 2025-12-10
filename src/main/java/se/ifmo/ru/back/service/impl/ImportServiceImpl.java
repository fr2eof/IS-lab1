package se.ifmo.ru.back.service.impl;

import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.dto.*;
import se.ifmo.ru.back.entity.*;
import se.ifmo.ru.back.entity.Weapon;
import se.ifmo.ru.back.exception.AccessDeniedException;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.exception.ValidationException;
import se.ifmo.ru.back.repository.ChapterRepository;
import se.ifmo.ru.back.repository.CoordinatesRepository;
import se.ifmo.ru.back.repository.ImportHistoryRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;
import se.ifmo.ru.back.service.ImportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImportServiceImpl implements ImportService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImportServiceImpl.class);

    private final SpaceMarineRepository spaceMarineRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final ChapterRepository chapterRepository;
    private final ImportHistoryRepository importHistoryRepository;
    private final Validator validator;
    private final LockManager lockManager;

    public ImportServiceImpl(
            SpaceMarineRepository spaceMarineRepository,
            CoordinatesRepository coordinatesRepository,
            ChapterRepository chapterRepository,
            ImportHistoryRepository importHistoryRepository,
            Validator validator,
            LockManager lockManager) {
        this.spaceMarineRepository = spaceMarineRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.chapterRepository = chapterRepository;
        this.importHistoryRepository = importHistoryRepository;
        this.validator = validator;
        this.lockManager = lockManager;
    }
    
    /**
     * Генерирует ключ блокировки для координат на основе x и y.
     * Используется для предотвращения создания дубликатов координат.
     */
    private String getCoordinatesLockKey(Float x, Double y) {
        return "coords_" + x + "_" + (y != null ? y : "null");
    }
    
    /**
     * Генерирует ключ блокировки для главы на основе name и marinesCount.
     * Используется для предотвращения создания дубликатов глав.
     */
    private String getChapterLockKey(String name, Integer marinesCount) {
        return "chapter_" + name + "_" + marinesCount;
    }
    
    /**
     * Генерирует ключ блокировки для SpaceMarine в главе.
     * Используется для предотвращения создания дубликатов маринов в одной главе.
     */
    private String getSpaceMarineLockKey(Chapter chapter, Coordinates coordinates, Integer health, Weapon weaponType) {
        String chapterPart;
        if (chapter == null) {
            chapterPart = "no_chapter";
        } else if (chapter.getId() != null) {
            chapterPart = "chapterId_" + chapter.getId();
        } else {
            chapterPart = "chapterNew_" + chapter.getName() + "_" + chapter.getMarinesCount();
        }

        String coordsPart;
        if (coordinates == null) {
            coordsPart = "no_coords";
        } else if (coordinates.getId() != null) {
            coordsPart = "coordsId_" + coordinates.getId();
        } else {
            coordsPart = "coordsNew_" + coordinates.getX() + "_" + coordinates.getY();
        }

        String weaponPart = weaponType != null ? weaponType.name() : "null";

        return "marine_" + chapterPart + "_health_" + health + "_weapon_" + weaponPart + "_" + coordsPart;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username) {
        // Глобальная последовательная обработка импортов
        return lockManager.executeWithLock("import_global", () -> importSpaceMarinesInternal(request, username, null));
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username, Long existingHistoryId) {
        // Глобальная последовательная обработка импортов
        return lockManager.executeWithLock("import_global", () -> importSpaceMarinesInternal(request, username, existingHistoryId));
    }
    
    /**
     * Внутренняя реализация импорта. Предполагается, что вызов защищен глобальной блокировкой.
     */
    private ImportResponseDTO importSpaceMarinesInternal(ImportRequestDTO request, String username, Long existingHistoryId) {
        logger.info("Начало импорта space marines. Пользователь: {}, количество объектов: {}", 
            username, request != null && request.spaceMarines() != null ? request.spaceMarines().size() : 0);
        
        // Используем существующую запись или создаем новую
        ImportHistory importHistory;
        if (existingHistoryId != null) {
            Optional<ImportHistory> existing = importHistoryRepository.findById(existingHistoryId);
            if (existing.isPresent()) {
                importHistory = existing.get();
                // Обновляем статус на PENDING, если он был изменен
                importHistory.setStatus(ImportStatus.PENDING);
                importHistory = importHistoryRepository.save(importHistory);
            } else {
                // Если запись не найдена, создаем новую
                importHistory = new ImportHistory();
                importHistory.setUsername(username);
                importHistory.setStatus(ImportStatus.PENDING);
                importHistory = importHistoryRepository.save(importHistory);
            }
        } else {
            // Создаем новую запись в истории импорта со статусом PENDING
            importHistory = new ImportHistory();
            importHistory.setUsername(username);
            importHistory.setStatus(ImportStatus.PENDING);
            importHistory = importHistoryRepository.save(importHistory);
        }

        try {
            // Валидация запроса
            Set<ConstraintViolation<ImportRequestDTO>> requestViolations = validator.validate(request);
            if (!requestViolations.isEmpty()) {
                String errorMessage = requestViolations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage("Ошибка валидации запроса: " + errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(), 
                        "FAILED", 
                        0, 
                        "Ошибка валидации",
                        errorMessage
                );
            }

            List<SpaceMarine> createdMarines = new ArrayList<>();
            List<String> validationErrors = new ArrayList<>();

            // Списки для отложенного сохранения - все сущности будут сохранены только в конце, если нет ошибок
            List<Coordinates> coordinatesToSave = new ArrayList<>();
            List<Chapter> chaptersToSave = new ArrayList<>();
            List<SpaceMarine> marinesToSave = new ArrayList<>();
            List<Long> existingChapterIdsToUpdate = new ArrayList<>(); // ID существующих глав, для которых нужно обновить счетчик

            logger.info("Начинаем обработку {} объектов space marines", request.spaceMarines().size());
            
            // Обрабатываем каждый SpaceMarine из запроса - НЕ сохраняем сразу, только валидируем и подготавливаем
            for (int idx = 0; idx < request.spaceMarines().size(); idx++) {
                final int i = idx; // Делаем final для использования в лямбдах
                SpaceMarineImportDTO importDTO = request.spaceMarines().get(i);
                logger.info("Обработка объекта #{}: name={}, health={}, heartCount={}", 
                    i + 1, importDTO.name(), importDTO.health(), importDTO.heartCount());
                
                try {
                    // Валидация DTO
                    Set<ConstraintViolation<SpaceMarineImportDTO>> dtoViolations = validator.validate(importDTO);
                    if (!dtoViolations.isEmpty()) {
                        String errorMsg = dtoViolations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        validationErrors.add("Объект #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }

                    // Валидация координат - должны быть либо coordinatesId, либо coordinates
                    if (importDTO.coordinatesId() == null && importDTO.coordinates() == null) {
                        validationErrors.add("Объект #" + (i + 1) + ": Необходимо указать либо coordinatesId, либо coordinates");
                        continue;
                    }
                    
                    if (importDTO.coordinatesId() != null && importDTO.coordinates() != null) {
                        validationErrors.add("Объект #" + (i + 1) + ": Нельзя указывать одновременно coordinatesId и coordinates");
                        continue;
                    }

                    // Валидация вложенных объектов, если указаны
                    if (importDTO.coordinates() != null) {
                        Set<ConstraintViolation<CoordinatesDTO>> coordViolations = validator.validate(importDTO.coordinates());
                        if (!coordViolations.isEmpty()) {
                            String errorMsg = coordViolations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining("; "));
                            validationErrors.add("Объект #" + (i + 1) + ", координаты: " + errorMsg);
                            continue;
                        }
                    }

                    if (importDTO.chapter() != null) {
                        Set<ConstraintViolation<ChapterDTO>> chapterViolations = validator.validate(importDTO.chapter());
                        if (!chapterViolations.isEmpty()) {
                            String errorMsg = chapterViolations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining("; "));
                            validationErrors.add("Объект #" + (i + 1) + ", глава: " + errorMsg);
                            continue;
                        }
                    }
                    
                    if (importDTO.chapterId() != null && importDTO.chapter() != null) {
                        validationErrors.add("Объект #" + (i + 1) + ": Нельзя указывать одновременно chapterId и chapter");
                        continue;
                    }

                    // Создаем или находим Coordinates
                    Coordinates coordinates;
                    if (importDTO.coordinatesId() != null) {
                        // Используем существующие координаты по ID
                        Optional<Coordinates> existingCoords = coordinatesRepository.findById(importDTO.coordinatesId());
                        if (existingCoords.isEmpty()) {
                            validationErrors.add("Объект #" + (i + 1) + ": Координаты с ID " + importDTO.coordinatesId() + " не найдены");
                            continue;
                        }
                        coordinates = existingCoords.get();
                    } else {
                        // Создаем новые координаты
                        CoordinatesDTO coordDTO = importDTO.coordinates();
                        
                        // Блокируем проверку уникальности координат на уровне приложения
                        String coordsLockKey = getCoordinatesLockKey(coordDTO.x(), coordDTO.y());
                        coordinates = lockManager.executeWithLock(coordsLockKey, () -> {
                            // Проверяем, не создали ли уже такие координаты в этой транзакции
                            Optional<Coordinates> existingInTransaction = coordinatesToSave.stream()
                                .filter(c -> c.getX().equals(coordDTO.x()) && 
                                           java.util.Objects.equals(c.getY(), coordDTO.y()))
                                .findFirst();
                            
                            if (existingInTransaction.isPresent()) {
                                return existingInTransaction.get();
                            }
                            
                            // Проверяем в БД с блокировкой (pessimistic locking)
                            Optional<Coordinates> existingInDb = coordinatesRepository.findByXAndYWithLock(
                                coordDTO.x(), coordDTO.y());
                            
                            if (existingInDb.isPresent()) {
                                return existingInDb.get();
                            }
                            
                            // Создаем новые координаты
                            Coordinates newCoords = new Coordinates();
                            newCoords.setX(coordDTO.x());
                            newCoords.setY(coordDTO.y());
                        
                        // Валидация сущности (включая кастомные валидаторы)
                            Set<ConstraintViolation<Coordinates>> coordViolations = validator.validate(newCoords);
                        if (!coordViolations.isEmpty()) {
                            String errorMsg = coordViolations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining("; "));
                                throw new ValidationException("Объект #" + (i + 1) + ", координаты: " + errorMsg);
                            }
                            
                            // Добавляем в список для отложенного сохранения
                            coordinatesToSave.add(newCoords);
                            logger.info("Координаты для объекта #{} подготовлены к сохранению: x={}, y={}", 
                                i + 1, newCoords.getX(), newCoords.getY());
                            
                            return newCoords;
                        });
                    }

                    // Создаем или находим Chapter
                    final java.util.concurrent.atomic.AtomicReference<Chapter> chapterRef = new java.util.concurrent.atomic.AtomicReference<>();
                    final java.util.concurrent.atomic.AtomicBoolean isNewChapterRef = new java.util.concurrent.atomic.AtomicBoolean(false);
                    
                    if (importDTO.chapterId() != null) {
                        // Используем существующую главу по ID
                        Optional<Chapter> existingChapter = chapterRepository.findById(importDTO.chapterId());
                        if (existingChapter.isEmpty()) {
                            validationErrors.add("Объект #" + (i + 1) + ": Глава с ID " + importDTO.chapterId() + " не найдена");
                            continue;
                        }
                        Chapter chapter = existingChapter.get();
                        // Проверяем, не превышен ли лимит маринов в главе
                        if (chapter.getMarinesCount() >= 1000) {
                            validationErrors.add("Объект #" + (i + 1) + ": Глава " + chapter.getName() + " уже содержит максимальное количество маринов (1000)");
                            continue;
                        }
                        // Существующая глава - запомним ID для обновления счетчика
                        existingChapterIdsToUpdate.add(chapter.getId());
                        chapterRef.set(chapter);
                    } else if (importDTO.chapter() != null) {
                        // Создаем новую главу или ищем по имени
                        ChapterDTO chapterDTO = importDTO.chapter();
                        
                        // Блокируем проверку уникальности главы на уровне приложения
                        String chapterLockKey = getChapterLockKey(chapterDTO.name(), chapterDTO.marinesCount());
                        Chapter chapter = lockManager.executeWithLock(chapterLockKey, () -> {
                            // Сначала ищем по имени (без блокировки, так как блокировка на уровне приложения)
                            Optional<Chapter> existingByName = chapterRepository.findByName(chapterDTO.name());
                            
                            if (existingByName.isPresent()) {
                                Chapter existing = existingByName.get();
                            // Проверяем, не превышен ли лимит маринов в главе
                                if (existing.getMarinesCount() >= 1000) {
                                    throw new ValidationException("Объект #" + (i + 1) + ": Глава " + existing.getName() + 
                                        " уже содержит максимальное количество маринов (1000)");
                                }
                                // Существующая глава - запомним ID для обновления счетчика
                                existingChapterIdsToUpdate.add(existing.getId());
                                return existing;
                            }
                            
                            // Проверяем, не создали ли уже такую главу в этой транзакции
                            Optional<Chapter> existingInTransaction = chaptersToSave.stream()
                                .filter(c -> c.getName().equals(chapterDTO.name()) && 
                                           c.getMarinesCount() == chapterDTO.marinesCount())
                                .findFirst();
                            
                            if (existingInTransaction.isPresent()) {
                                return existingInTransaction.get();
                            }
                            
                            // Проверяем в БД с блокировкой (pessimistic locking)
                            Optional<Chapter> existingInDb = chapterRepository.findByNameAndMarinesCountWithLock(
                                chapterDTO.name(), chapterDTO.marinesCount());
                            
                            if (existingInDb.isPresent()) {
                                Chapter existing = existingInDb.get();
                                if (existing.getMarinesCount() >= 1000) {
                                    throw new ValidationException("Объект #" + (i + 1) + ": Глава " + existing.getName() + 
                                        " уже содержит максимальное количество маринов (1000)");
                                }
                                existingChapterIdsToUpdate.add(existing.getId());
                                return existing;
                            }
                            
                            // При создании нового Chapter счетчик должен быть 1, так как мы сразу добавляем марина
                            Chapter newChapter = new Chapter();
                            newChapter.setName(chapterDTO.name());
                            newChapter.setMarinesCount(1);
                            
                            // Валидация сущности (включая кастомные валидаторы)
                            Set<ConstraintViolation<Chapter>> chapterViolations = validator.validate(newChapter);
                            if (!chapterViolations.isEmpty()) {
                                String errorMsg = chapterViolations.stream()
                                        .map(ConstraintViolation::getMessage)
                                        .collect(Collectors.joining("; "));
                                throw new ValidationException("Объект #" + (i + 1) + ", глава: " + errorMsg);
                            }
                            
                            // Добавляем в список для отложенного сохранения
                            chaptersToSave.add(newChapter);
                            logger.info("Глава для объекта #{} подготовлена к сохранению: name={}, marinesCount={}", 
                                i + 1, newChapter.getName(), newChapter.getMarinesCount());
                            
                            return newChapter;
                        });
                        chapterRef.set(chapter);
                        isNewChapterRef.set(chaptersToSave.contains(chapter));
                    }
                    
                    final Chapter chapter = chapterRef.get();
                    final boolean isNewChapter = isNewChapterRef.get();

                    // Создаем SpaceMarine
                    SpaceMarine spaceMarine = new SpaceMarine();
                    spaceMarine.setName(importDTO.name());
                    spaceMarine.setCoordinates(coordinates);
                    spaceMarine.setChapter(chapter);
                    spaceMarine.setHealth(importDTO.health());
                    spaceMarine.setHeartCount(importDTO.heartCount());
                    
                    // Устанавливаем creationDate перед валидацией
                    spaceMarine.setCreationDate(java.time.ZonedDateTime.now());
                    logger.info("Установлен creationDate для объекта #{}: {}", i + 1, spaceMarine.getCreationDate());

                    // Обработка категории
                    if (importDTO.category() != null && !importDTO.category().trim().isEmpty()) {
                        try {
                            spaceMarine.setCategory(AstartesCategory.valueOf(importDTO.category()));
                        } catch (IllegalArgumentException e) {
                            logger.error("Ошибка установки категории для объекта #{}: {}", i + 1, importDTO.category(), e);
                            throw new ValidationException("Неверная категория: " + importDTO.category());
                        }
                    }

                    // Обработка оружия
                    if (importDTO.weaponType() != null && !importDTO.weaponType().trim().isEmpty()) {
                        try {
                            spaceMarine.setWeaponType(Weapon.valueOf(importDTO.weaponType()));
                        } catch (IllegalArgumentException e) {
                            logger.error("Ошибка установки типа оружия для объекта #{}: {}", i + 1, importDTO.weaponType(), e);
                            throw new ValidationException("Неверный тип оружия: " + importDTO.weaponType());
                        }
                    }

                    // Валидация сущности (включая кастомные валидаторы)
                    logger.info("Валидация SpaceMarine для объекта #{}: name={}", i + 1, spaceMarine.getName());
                    Set<ConstraintViolation<SpaceMarine>> marineViolations = validator.validate(spaceMarine);
                    if (!marineViolations.isEmpty()) {
                        String errorMsg = marineViolations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        logger.error("Ошибка валидации SpaceMarine для объекта #{}: {}", i + 1, errorMsg);
                        validationErrors.add("Объект #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }
                    logger.info("Валидация SpaceMarine для объекта #{} прошла успешно", i + 1);

                    // Проверка уникальности SpaceMarine в главе с блокировкой на уровне приложения
                    if (chapter != null) {
                        Weapon weaponEnum = spaceMarine.getWeaponType();
                        String marineLockKey = getSpaceMarineLockKey(
                            chapter,
                            coordinates,
                            spaceMarine.getHealth(),
                            weaponEnum
                        ); 
                        
                        lockManager.executeWithLock(marineLockKey, () -> {
                            // Проверяем, не создали ли уже такого марина в этой транзакции
                            boolean duplicateInTransaction = marinesToSave.stream()
                                .anyMatch(m -> {
                                    // Сравнение главы: если есть id, то по id, иначе по name+marinesCount
                                    boolean sameChapter = (m.getChapter() == null && chapter == null) ||
                                            (m.getChapter() != null && chapter != null &&
                                             ((m.getChapter().getId() != null && chapter.getId() != null &&
                                               m.getChapter().getId().equals(chapter.getId()))
                                              ||
                                              (m.getChapter().getId() == null && chapter.getId() == null &&
                                               m.getChapter().getName().equals(chapter.getName()) &&
                                               m.getChapter().getMarinesCount() == chapter.getMarinesCount())));

                                    // Сравнение координат: если есть id, то по id, иначе по x/y
                                    boolean sameCoords = (m.getCoordinates() == null && coordinates == null) ||
                                            (m.getCoordinates() != null && coordinates != null &&
                                             ((m.getCoordinates().getId() != null && coordinates.getId() != null &&
                                               m.getCoordinates().getId().equals(coordinates.getId()))
                                              ||
                                              (m.getCoordinates().getId() == null && coordinates.getId() == null &&
                                               m.getCoordinates().getX().equals(coordinates.getX()) &&
                                               java.util.Objects.equals(m.getCoordinates().getY(), coordinates.getY()))));

                                    boolean sameHealth = m.getHealth().equals(spaceMarine.getHealth());

                                    String mWeapon = m.getWeaponType() != null ? m.getWeaponType().name() : null;
                                    String weaponStr = weaponEnum != null ? weaponEnum.name() : null;
                                    boolean sameWeapon = java.util.Objects.equals(mWeapon, weaponStr);

                                    return sameChapter && sameCoords && sameHealth && sameWeapon;
                                });
                            
                            if (duplicateInTransaction) {
                                throw new ValidationException("Объект #" + (i + 1) + 
                                    ": Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                            }
                            
                            // Проверяем в БД с блокировкой (pessimistic locking)
                            List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                                chapter.getId(),
                                spaceMarine.getHealth(),
                                spaceMarine.getWeaponType(),
                                coordinates.getId()
                            );
                            
                            if (!duplicates.isEmpty()) {
                                throw new ValidationException("Объект #" + (i + 1) + 
                                    ": Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                            }
                            
                            // Все проверки прошли - добавляем в список для сохранения
                            marinesToSave.add(spaceMarine);
                            logger.info("SpaceMarine для объекта #{} подготовлен к сохранению: name={}", 
                                i + 1, spaceMarine.getName());
                            
                            // Если это существующая глава, запомним ID для обновления счетчика
                            if (!isNewChapter) {
                                if (!existingChapterIdsToUpdate.contains(chapter.getId())) {
                                    existingChapterIdsToUpdate.add(chapter.getId());
                                }
                            }
                        });
                    } else {
                        // Если глава не указана, просто добавляем в список
                        marinesToSave.add(spaceMarine);
                        logger.info("SpaceMarine для объекта #{} подготовлен к сохранению: name={}", 
                            i + 1, spaceMarine.getName());
                    }

                } catch (ValidationException e) {
                    // ValidationException уже содержит понятное сообщение
                    String errorMessage = e.getMessage();
                    logger.error("Ошибка валидации при обработке объекта #{}: {}", i + 1, errorMessage);
                    validationErrors.add("Объект #" + (i + 1) + ": " + errorMessage);
                } catch (Exception e) {
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.trim().isEmpty()) {
                        errorMessage = e.getClass().getSimpleName();
                    }
                    logger.error("ОШИБКА при обработке объекта #{}: {}", i + 1, errorMessage, e);
                    logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), errorMessage);
                    if (e.getCause() != null) {
                        logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                        logger.error("Stack trace причины:", e.getCause());
                    }
                    logger.error("Полный stack trace ошибки:", e);
                    validationErrors.add("Объект #" + (i + 1) + ": " + errorMessage);
                }
            }

            // Если были ошибки валидации, устанавливаем статус FAILED и откатываем транзакцию
            if (!validationErrors.isEmpty()) {
                String errorMessage = String.join("; ", validationErrors);
                logger.error("Обнаружены ошибки валидации при импорте: {}", errorMessage);
                logger.error("Количество ошибок: {}", validationErrors.size());
                logger.error("Список ошибок валидации: {}", errorMessage);
                logger.error("Сущности НЕ будут сохранены из-за ошибок валидации");
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                // Выбрасываем исключение для отката транзакции - все сущности будут откачены
                throw new ValidationException("Ошибки при импорте: " + errorMessage);
            }

            // Если не было создано ни одного объекта
            if (marinesToSave.isEmpty()) {
                String errorMessage = "Не было создано ни одного объекта";
                logger.error("Не было создано ни одного объекта при импорте");
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                // Выбрасываем исключение для отката транзакции
                throw new ValidationException(errorMessage);
            }
            
            // Все валидации прошли успешно - теперь сохраняем все сущности в правильном порядке
            logger.info("Все валидации прошли успешно. Начинаем сохранение сущностей:");
            logger.info("Координат для сохранения: {}", coordinatesToSave.size());
            logger.info("Глав для сохранения: {}", chaptersToSave.size());
            logger.info("SpaceMarines для сохранения: {}", marinesToSave.size());
            
            // Сохраняем координаты
            for (Coordinates coord : coordinatesToSave) {
                coord = coordinatesRepository.save(coord);
                logger.info("Координаты сохранены: id={}, x={}, y={}", coord.getId(), coord.getX(), coord.getY());
            }
            
            // Сохраняем главы
            for (Chapter ch : chaptersToSave) {
                ch = chapterRepository.save(ch);
                logger.info("Глава сохранена: id={}, name={}, marinesCount={}", 
                    ch.getId(), ch.getName(), ch.getMarinesCount());
            }
            
            // Сохраняем SpaceMarines
            for (SpaceMarine marine : marinesToSave) {
                marine = spaceMarineRepository.save(marine);
                createdMarines.add(marine);
                logger.info("SpaceMarine сохранен: id={}, name={}", marine.getId(), marine.getName());
            }
            
            // Обновляем счетчики в существующих главах
            for (Long chapterId : existingChapterIdsToUpdate) {
                logger.info("Обновление счетчика маринов в главе: chapterId={}", chapterId);
                chapterRepository.addMarineToChapter(chapterId);
            }

            // Успешный импорт
            importHistory.setStatus(ImportStatus.SUCCESS);
            importHistory.setCreatedObjectsCount(createdMarines.size());
            importHistory.setErrorMessage(null);
            importHistory = importHistoryRepository.save(importHistory);

            return new ImportResponseDTO(
                    importHistory.getId(), 
                    "SUCCESS", 
                    createdMarines.size(), 
                    "Успешно импортировано " + createdMarines.size() + " объектов"
            );

        } catch (ValidationException e) {
            // Для ValidationException не нужно логировать, так как это ожидаемая ошибка валидации
            // Обновляем статус на FAILED при ошибке валидации
            importHistory.setStatus(ImportStatus.FAILED);
            if (importHistory.getErrorMessage() == null) {
                importHistory.setErrorMessage(e.getMessage());
            }
            importHistory = importHistoryRepository.save(importHistory);
            throw e; // Откатываем транзакцию
        } catch (Exception e) {
            // Обновляем статус на FAILED при любой другой ошибке
            logger.error("ОШИБКА при импорте space marines: пользователь={}", username, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            importHistory.setStatus(ImportStatus.FAILED);
            if (importHistory.getErrorMessage() == null) {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    errorMessage = "Неизвестная ошибка: " + e.getClass().getSimpleName();
                }
                importHistory.setErrorMessage(errorMessage);
            }
            importHistory = importHistoryRepository.save(importHistory);
            throw e; // Откатываем транзакцию
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importFromFile(String fileContent, String username) {
        // Создаем запись в истории импорта со статусом PENDING
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportStatus.PENDING);
        importHistory = importHistoryRepository.save(importHistory);
        
        try {
            // Парсим JSON
            ImportRequestDTO request;
            try {
                request = parseJsonFile(fileContent);
            } catch (Exception e) {
                // Ошибка парсинга JSON - сохраняем в историю
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    errorMessage = "Ошибка парсинга JSON";
                }
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                
                return new ImportResponseDTO(
                        importHistory.getId(), 
                        "FAILED", 
                        0, 
                        "Ошибка формата файла",
                        errorMessage
                );
            }
            
            // Валидация структуры
            if (request.spaceMarines() == null || request.spaceMarines().isEmpty()) {
                String errorMessage = "Файл не содержит объектов для импорта";
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                
                return new ImportResponseDTO(
                        importHistory.getId(), 
                        "FAILED", 
                        0, 
                        "Ошибка структуры файла",
                        errorMessage
                );
            }
            
            // Используем существующую запись для импорта
            Long existingHistoryId = importHistory.getId();
            
            // Импортируем данные, используя существующую запись истории
            return importSpaceMarines(request, username, existingHistoryId);
            
        } catch (Exception e) {
            // Обновляем статус на FAILED при любой ошибке
            importHistory.setStatus(ImportStatus.FAILED);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Неизвестная ошибка при импорте";
            }
            importHistory.setErrorMessage(errorMessage);
            importHistory = importHistoryRepository.save(importHistory);
            
            return new ImportResponseDTO(
                    importHistory.getId(), 
                    "FAILED", 
                    0, 
                    "Ошибка импорта",
                    errorMessage
            );
        }
    }
    
    private ImportRequestDTO parseJsonFile(String fileContent) throws Exception {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        
        try {
            // Используем Jakarta JSON для парсинга
            jakarta.json.JsonReader jsonReader = jakarta.json.Json.createReader(
                    new java.io.StringReader(fileContent));
            jakarta.json.JsonValue jsonValue = jsonReader.read();
            jsonReader.close();
            
            // Проверяем, что это объект
            if (jsonValue.getValueType() != jakarta.json.JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("Файл должен содержать JSON объект");
            }
            
            jakarta.json.JsonObject jsonObject = jsonValue.asJsonObject();
            
            // Проверяем наличие поля spaceMarines
            if (!jsonObject.containsKey("spaceMarines")) {
                throw new IllegalArgumentException("Файл должен содержать поле 'spaceMarines'");
            }
            
            jakarta.json.JsonValue spaceMarinesValue = jsonObject.get("spaceMarines");
            if (spaceMarinesValue.getValueType() != jakarta.json.JsonValue.ValueType.ARRAY) {
                throw new IllegalArgumentException("Поле 'spaceMarines' должно быть массивом");
            }
            
            jakarta.json.JsonArray spaceMarinesArray = spaceMarinesValue.asJsonArray();
            if (spaceMarinesArray.isEmpty()) {
                throw new IllegalArgumentException("Массив 'spaceMarines' не может быть пустым");
            }
            
            // Преобразуем JSON в DTO
            List<SpaceMarineImportDTO> spaceMarines = new ArrayList<>();
            
            for (int i = 0; i < spaceMarinesArray.size(); i++) {
                jakarta.json.JsonValue marineValue = spaceMarinesArray.get(i);
                if (marineValue.getValueType() != jakarta.json.JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("Элемент #" + (i + 1) + " массива spaceMarines должен быть объектом");
                }
                jakarta.json.JsonObject marineJson = marineValue.asJsonObject();
                SpaceMarineImportDTO importDTO = parseSpaceMarineDTO(marineJson);
                spaceMarines.add(importDTO);
            }
            
            return new ImportRequestDTO(spaceMarines);
            
        } catch (jakarta.json.JsonException e) {
            // Ошибка парсинга JSON
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getSimpleName();
            }
            throw new IllegalArgumentException(formatJsonError(errorMsg));
        } catch (IllegalArgumentException e) {
            // Пробрасываем наши ошибки как есть
            throw e;
        } catch (Exception e) {
            // Любая другая ошибка
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "Неизвестная ошибка: " + e.getClass().getSimpleName();
            }
            throw new IllegalArgumentException(formatJsonError(errorMsg));
        }
    }
    
    private SpaceMarineImportDTO parseSpaceMarineDTO(JsonObject json) {
        String name = json.containsKey("name") ? json.getString("name") : null;
        
        // Обработка coordinatesId или coordinates
        Long coordinatesId = null;
        CoordinatesDTO coordinatesDTO = null;
        if (json.containsKey("coordinatesId")) {
            coordinatesId = json.getJsonNumber("coordinatesId").longValue();
        } else if (json.containsKey("coordinates")) {
            jakarta.json.JsonObject coordsJson = json.getJsonObject("coordinates");
            Float x = coordsJson.containsKey("x") ? (float) coordsJson.getJsonNumber("x").doubleValue() : null;
            Double y = coordsJson.containsKey("y") && !coordsJson.isNull("y") 
                    ? coordsJson.getJsonNumber("y").doubleValue() 
                    : null;
            coordinatesDTO = new CoordinatesDTO(null, x, y);
        }
        
        // Обработка chapterId или chapter
        Long chapterId = null;
        ChapterDTO chapterDTO = null;
        if (json.containsKey("chapterId")) {
            chapterId = json.getJsonNumber("chapterId").longValue();
        } else if (json.containsKey("chapter")) {
            jakarta.json.JsonObject chapterJson = json.getJsonObject("chapter");
            String chapterName = chapterJson.containsKey("name") ? chapterJson.getString("name") : null;
            Integer marinesCount = chapterJson.containsKey("marinesCount") 
                    ? chapterJson.getInt("marinesCount") 
                    : null;
            chapterDTO = new ChapterDTO(null, chapterName, marinesCount);
        }
        
        Integer health = json.containsKey("health") ? json.getInt("health") : null;
        Integer heartCount = json.containsKey("heartCount") ? json.getInt("heartCount") : null;
        String category = json.containsKey("category") ? json.getString("category") : null;
        String weaponType = json.containsKey("weaponType") ? json.getString("weaponType") : null;
        
        return new SpaceMarineImportDTO(
                name,
                coordinatesId,
                coordinatesDTO,
                chapterId,
                chapterDTO,
                health,
                heartCount,
                category,
                weaponType
        );
    }
    
    private String formatJsonError(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return "Ошибка парсинга JSON";
        }
        
        String msg = errorMessage.trim();
        
        // Упрощаем сообщение об ошибке для пользователя
        if (msg.contains("Unexpected token")) {
            // Извлекаем информацию о токене, если возможно
            if (msg.contains("'") && msg.indexOf("'") != msg.lastIndexOf("'")) {
                int start = msg.indexOf("'") + 1;
                int end = msg.indexOf("'", start);
                if (end > start && end - start < 20) {
                    String token = msg.substring(start, end);
                    return "Неверный формат JSON. Неожиданный символ: '" + token + "'. Проверьте синтаксис файла.";
                }
            }
            return "Неверный формат JSON. Проверьте синтаксис файла.";
        }
        if (msg.contains("not valid JSON") || msg.contains("Invalid JSON")) {
            return "Файл не является валидным JSON";
        }
        if (msg.contains("EOF") || msg.contains("End of input")) {
            return "Файл обрезан или поврежден. Проверьте, что файл содержит полный JSON";
        }
        if (msg.contains("Expected")) {
            return "Неверный формат JSON. Проверьте структуру файла.";
        }
        
        // Обрезаем слишком длинные сообщения и убираем технические детали
        if (msg.length() > 150) {
            msg = msg.substring(0, 147) + "...";
        }
        
        return msg;
    }

    @Transactional
    @Override
    public ImportResponseDTO recordImportError(String username, String errorMessage) {
        // Создаем запись в истории импорта со статусом FAILED
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportStatus.FAILED);
        importHistory.setErrorMessage(errorMessage);
        importHistory.setCreatedObjectsCount(0);
        importHistory = importHistoryRepository.save(importHistory);
        
        return new ImportResponseDTO(
                importHistory.getId(), 
                "FAILED", 
                0, 
                "Ошибка импорта",
                errorMessage
        );
    }

    @Override
    public List<ImportHistoryDTO> getImportHistory(String username, boolean isAdmin) {
        List<ImportHistory> history;
        if (isAdmin) {
            history = importHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            history = importHistoryRepository.findByUsernameOrderByCreatedAtDesc(username);
        }
        return history.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ImportHistoryDTO> getImportHistory(String username, boolean isAdmin, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ImportHistory> history;
        
        if (isAdmin) {
            history = importHistoryRepository.findAll(pageable);
        } else {
            history = importHistoryRepository.findByUsernameOrderByCreatedAtDesc(username, pageable);
        }
        
        List<ImportHistoryDTO> dtos = history.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, history.getTotalElements());
    }

    @Override
    public ImportHistoryDTO getImportHistoryById(Long id, String username, boolean isAdmin) {
        Optional<ImportHistory> history = importHistoryRepository.findById(id);
        if (history.isPresent()) {
            ImportHistory ih = history.get();
            // Проверяем права доступа
            if (!isAdmin && !ih.getUsername().equals(username)) {
                throw new AccessDeniedException("Доступ запрещен");
            }
            return toDTO(ih);
        }
        throw new EntityNotFoundException("ImportHistory", id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importCoordinates(ImportCoordinatesRequestDTO request, String username) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportStatus.PENDING);
        importHistory = importHistoryRepository.save(importHistory);

        try {
            Set<ConstraintViolation<ImportCoordinatesRequestDTO>> requestViolations = validator.validate(request);
            if (!requestViolations.isEmpty()) {
                String errorMessage = requestViolations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage("Ошибка валидации запроса: " + errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибка валидации",
                        errorMessage
                );
            }

            List<Coordinates> createdCoordinates = new ArrayList<>();
            List<String> validationErrors = new ArrayList<>();

            for (int i = 0; i < request.coordinates().size(); i++) {
                CoordinatesDTO coordDTO = request.coordinates().get(i);
                
                try {
                    Set<ConstraintViolation<CoordinatesDTO>> violations = validator.validate(coordDTO);
                    if (!violations.isEmpty()) {
                        String errorMsg = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        validationErrors.add("Координаты #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }

                    Coordinates coordinates = new Coordinates();
                    coordinates.setX(coordDTO.x());
                    coordinates.setY(coordDTO.y());
                    
                    // Валидация сущности (включая кастомные валидаторы)
                    Set<ConstraintViolation<Coordinates>> coordViolations = validator.validate(coordinates);
                    if (!coordViolations.isEmpty()) {
                        String errorMsg = coordViolations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        validationErrors.add("Координаты #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }
                    
                    coordinates = coordinatesRepository.save(coordinates);
                    createdCoordinates.add(coordinates);

                } catch (Exception e) {
                    validationErrors.add("Координаты #" + (i + 1) + ": " + e.getMessage());
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = String.join("; ", validationErrors);
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибки при импорте",
                        errorMessage
                );
            }

            if (createdCoordinates.isEmpty()) {
                String errorMessage = "Не было создано ни одного объекта";
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибка импорта",
                        errorMessage
                );
            }

            importHistory.setStatus(ImportStatus.SUCCESS);
            importHistory.setCreatedObjectsCount(createdCoordinates.size());
            importHistory.setErrorMessage(null);
            importHistory = importHistoryRepository.save(importHistory);

            return new ImportResponseDTO(
                    importHistory.getId(),
                    "SUCCESS",
                    createdCoordinates.size(),
                    "Успешно импортировано " + createdCoordinates.size() + " координат"
            );

        } catch (Exception e) {
            importHistory.setStatus(ImportStatus.FAILED);
            if (importHistory.getErrorMessage() == null) {
                importHistory.setErrorMessage(e.getMessage());
            }
            importHistory = importHistoryRepository.save(importHistory);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importChapters(ImportChaptersRequestDTO request, String username) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportStatus.PENDING);
        importHistory = importHistoryRepository.save(importHistory);

        try {
            Set<ConstraintViolation<ImportChaptersRequestDTO>> requestViolations = validator.validate(request);
            if (!requestViolations.isEmpty()) {
                String errorMessage = requestViolations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage("Ошибка валидации запроса: " + errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибка валидации",
                        errorMessage
                );
            }

            List<Chapter> createdChapters = new ArrayList<>();
            List<String> validationErrors = new ArrayList<>();

            for (int i = 0; i < request.chapters().size(); i++) {
                ChapterDTO chapterDTO = request.chapters().get(i);
                
                try {
                    Set<ConstraintViolation<ChapterDTO>> violations = validator.validate(chapterDTO);
                    if (!violations.isEmpty()) {
                        String errorMsg = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        validationErrors.add("Глава #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }

                    Chapter chapter = new Chapter();
                    chapter.setName(chapterDTO.name());
                    chapter.setMarinesCount(chapterDTO.marinesCount());
                    
                    // Валидация сущности (включая кастомные валидаторы)
                    Set<ConstraintViolation<Chapter>> chapterViolations = validator.validate(chapter);
                    if (!chapterViolations.isEmpty()) {
                        String errorMsg = chapterViolations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        validationErrors.add("Глава #" + (i + 1) + ": " + errorMsg);
                        continue;
                    }
                    
                    chapter = chapterRepository.save(chapter);
                    createdChapters.add(chapter);

                } catch (Exception e) {
                    validationErrors.add("Глава #" + (i + 1) + ": " + e.getMessage());
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = String.join("; ", validationErrors);
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибки при импорте",
                        errorMessage
                );
            }

            if (createdChapters.isEmpty()) {
                String errorMessage = "Не было создано ни одного объекта";
                importHistory.setStatus(ImportStatus.FAILED);
                importHistory.setErrorMessage(errorMessage);
                importHistory = importHistoryRepository.save(importHistory);
                return new ImportResponseDTO(
                        importHistory.getId(),
                        "FAILED",
                        0,
                        "Ошибка импорта",
                        errorMessage
                );
            }

            importHistory.setStatus(ImportStatus.SUCCESS);
            importHistory.setCreatedObjectsCount(createdChapters.size());
            importHistory.setErrorMessage(null);
            importHistory = importHistoryRepository.save(importHistory);

            return new ImportResponseDTO(
                    importHistory.getId(),
                    "SUCCESS",
                    createdChapters.size(),
                    "Успешно импортировано " + createdChapters.size() + " глав"
            );

        } catch (Exception e) {
            importHistory.setStatus(ImportStatus.FAILED);
            if (importHistory.getErrorMessage() == null) {
                importHistory.setErrorMessage(e.getMessage());
            }
            importHistory = importHistoryRepository.save(importHistory);
            throw e;
        }
    }

    private ImportHistoryDTO toDTO(ImportHistory entity) {
        return new ImportHistoryDTO(
                entity.getId(),
                entity.getUsername(),
                entity.getStatus().name(),
                entity.getCreatedObjectsCount(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}
