package se.ifmo.ru.back.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.dto.*;
import se.ifmo.ru.back.repository.ChapterRepository;
import se.ifmo.ru.back.repository.CoordinatesRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;
import se.ifmo.ru.back.entity.*;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.exception.ValidationException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.ChapterService;
import se.ifmo.ru.back.service.CoordinatesService;
import se.ifmo.ru.back.service.SpaceMarineService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceMarineServiceImpl implements SpaceMarineService {

    private static final Logger logger = LoggerFactory.getLogger(SpaceMarineServiceImpl.class);

    private final SpaceMarineRepository spaceMarineRepository;
    private final ChapterRepository chapterRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final ChapterService chapterService;
    private final CoordinatesService coordinatesService;
    private final SpaceMarineMapper spaceMarineMapper;
    private final Validator validator;
    private final EntityManager entityManager;

    public SpaceMarineServiceImpl(
            SpaceMarineRepository spaceMarineRepository,
            ChapterRepository chapterRepository,
            CoordinatesRepository coordinatesRepository,
            ChapterService chapterService,
            CoordinatesService coordinatesService,
            SpaceMarineMapper spaceMarineMapper,
            Validator validator,
            EntityManager entityManager) {
        this.spaceMarineRepository = spaceMarineRepository;
        this.chapterRepository = chapterRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.chapterService = chapterService;
        this.coordinatesService = coordinatesService;
        this.spaceMarineMapper = spaceMarineMapper;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public SpaceMarine createSpaceMarine(SpaceMarine spaceMarine) {
        logger.info("Создание space marine: name={}, health={}, heartCount={}", 
            spaceMarine.getName(), spaceMarine.getHealth(), spaceMarine.getHeartCount());
        
        try {
            // Устанавливаем creationDate, если он не указан
            if (spaceMarine.getCreationDate() == null) {
                spaceMarine.setCreationDate(ZonedDateTime.now());
                logger.info("Установлен creationDate: {}", spaceMarine.getCreationDate());
            }
            
            // Проверка уникальности с блокировкой для предотвращения race conditions
            if (spaceMarine.getChapter() != null && spaceMarine.getChapter().getId() != null && 
                spaceMarine.getHealth() != null && spaceMarine.getCoordinates() != null) {
                logger.info("Проверка уникальности: chapterId={}, health={}, weapon={}, coordinatesId={}", 
                    spaceMarine.getChapter().getId(), spaceMarine.getHealth(), 
                    spaceMarine.getWeaponType(), spaceMarine.getCoordinates().getId());
                List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                        spaceMarine.getChapter().getId(),
                        spaceMarine.getHealth(),
                        spaceMarine.getWeaponType(),
                        spaceMarine.getCoordinates().getId());
                if (!duplicates.isEmpty()) {
                    logger.warn("Попытка создать дубликат space marine: name={}, chapterId={}, health={}", 
                        spaceMarine.getName(), spaceMarine.getChapter().getId(), spaceMarine.getHealth());
                    throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                }
                logger.info("Проверка уникальности прошла успешно");
            }
            
            // Валидация сущности (включая кастомные валидаторы)
            logger.info("Валидация space marine: name={}", spaceMarine.getName());
            Set<ConstraintViolation<SpaceMarine>> violations = validator.validate(spaceMarine);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                logger.error("Ошибка валидации space marine: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            logger.info("Валидация space marine прошла успешно");
            
            if (spaceMarine.getChapter() != null && spaceMarine.getChapter().getId() != null) {
                logger.info("Обновление счетчика маринов в главе: chapterId={}", spaceMarine.getChapter().getId());
                Optional<Chapter> existingChapter = chapterRepository.findById(spaceMarine.getChapter().getId());
                if (existingChapter.isPresent()) {
                    spaceMarine.setChapter(existingChapter.get());
                    chapterRepository.addMarineToChapter(existingChapter.get().getId());
                }
            }
            
            logger.info("Сохранение space marine в БД: name={}, creationDate={}", 
                spaceMarine.getName(), spaceMarine.getCreationDate());
            SpaceMarine saved = spaceMarineRepository.save(spaceMarine);
            logger.info("Space marine успешно создан: id={}, name={}", saved.getId(), saved.getName());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при создании space marine: name={}", spaceMarine.getName(), e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }
    
    @Transactional
    public SpaceMarine createSpaceMarineFromDTO(SpaceMarineDTO dto) {
        logger.info("Создание space marine из DTO: name={}, health={}, heartCount={}, coordinatesId={}, chapterId={}", 
            dto.name(), dto.health(), dto.heartCount(), dto.coordinatesId(), dto.chapterId());
        
        try {
            SpaceMarine spaceMarine = new SpaceMarine();
            spaceMarine.setName(dto.name());
            spaceMarine.setHealth(dto.health());
            spaceMarine.setHeartCount(dto.heartCount());
            
            // Обработка категории
            if (dto.category() != null && !dto.category().trim().isEmpty()) {
                logger.info("Установка категории: {}", dto.category());
                try {
                    spaceMarine.setCategory(AstartesCategory.valueOf(dto.category()));
                } catch (IllegalArgumentException e) {
                    logger.error("Неверная категория: {}", dto.category());
                    throw new IllegalArgumentException("Неверная категория: " + dto.category());
                }
            }
            
            // Обработка оружия
            if (dto.weaponType() != null && !dto.weaponType().trim().isEmpty()) {
                logger.info("Установка типа оружия: {}", dto.weaponType());
                try {
                    spaceMarine.setWeaponType(Weapon.valueOf(dto.weaponType()));
                } catch (IllegalArgumentException e) {
                    logger.error("Неверный тип оружия: {}", dto.weaponType());
                    throw new IllegalArgumentException("Неверный тип оружия: " + dto.weaponType());
                }
            }
            
            // Обработка координат (обязательно)
            if (dto.coordinatesId() == null) {
                logger.error("Координаты не указаны");
                throw new IllegalArgumentException("Координаты обязательны");
            }
            logger.info("Поиск координат с ID: {}", dto.coordinatesId());
            Optional<Coordinates> coordinates = coordinatesRepository.findById(dto.coordinatesId());
            if (!coordinates.isPresent()) {
                logger.error("Координаты с ID {} не найдены", dto.coordinatesId());
                throw new EntityNotFoundException("Coordinates", dto.coordinatesId());
            }
            spaceMarine.setCoordinates(coordinates.get());
            logger.info("Координаты найдены: id={}, x={}, y={}", 
                coordinates.get().getId(), coordinates.get().getX(), coordinates.get().getY());
            
            // Устанавливаем creationDate, если он не указан (для валидации)
            if (spaceMarine.getCreationDate() == null) {
                spaceMarine.setCreationDate(ZonedDateTime.now());
                logger.info("Установлен creationDate: {}", spaceMarine.getCreationDate());
            }
            
            // Обработка главы (необязательно)
            if (dto.chapterId() != null) {
                logger.info("Поиск главы с ID: {}", dto.chapterId());
                Optional<Chapter> chapter = chapterRepository.findById(dto.chapterId());
                if (chapter.isPresent()) {
                    spaceMarine.setChapter(chapter.get());
                    logger.info("Глава найдена: id={}, name={}", chapter.get().getId(), chapter.get().getName());
                } else {
                    logger.error("Глава с ID {} не найдена", dto.chapterId());
                    throw new EntityNotFoundException("Chapter", dto.chapterId());
                }
            }
            
            // Валидация сущности (включая кастомные валидаторы)
            logger.info("Валидация space marine из DTO: name={}", spaceMarine.getName());
            Set<ConstraintViolation<SpaceMarine>> violations = validator.validate(spaceMarine);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                logger.error("Ошибка валидации space marine из DTO: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            logger.info("Валидация space marine из DTO прошла успешно");
            
            // Сначала сохраняем SpaceMarine
            logger.info("Сохранение space marine в БД: name={}, health={}, creationDate={}", 
                spaceMarine.getName(), spaceMarine.getHealth(), spaceMarine.getCreationDate());
            try {
                SpaceMarine savedSpaceMarine = spaceMarineRepository.save(spaceMarine);
                logger.info("Space marine успешно сохранен в БД с ID: {}", savedSpaceMarine.getId());
                
                // Затем обновляем счетчик в Chapter
                if (dto.chapterId() != null) {
                    logger.info("Обновление счетчика маринов в главе: chapterId={}", dto.chapterId());
                    chapterRepository.addMarineToChapter(dto.chapterId());
                }
                
                logger.info("Space marine успешно создан из DTO: id={}, name={}", 
                    savedSpaceMarine.getId(), savedSpaceMarine.getName());
                return savedSpaceMarine;
            } catch (Exception saveException) {
                logger.error("КРИТИЧЕСКАЯ ОШИБКА при сохранении space marine в БД", saveException);
                logger.error("Детали ошибки сохранения: класс={}, сообщение={}", 
                    saveException.getClass().getName(), saveException.getMessage());
                if (saveException.getCause() != null) {
                    logger.error("Причина ошибки сохранения: {} - {}", 
                        saveException.getCause().getClass().getName(), saveException.getCause().getMessage());
                }
                throw saveException;
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при создании space marine из DTO: name={}", dto.name(), e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    public Optional<SpaceMarine> getSpaceMarineById(Integer id) {
        return spaceMarineRepository.findById(id);
    }

    public List<SpaceMarine> getAllSpaceMarines() {
        return spaceMarineRepository.findAllWithRelations();
    }

    public List<SpaceMarine> getSpaceMarines(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SpaceMarine> pageResult = spaceMarineRepository.findAllWithRelations(pageable);
        return pageResult.getContent();
    }

    public List<SpaceMarine> getSpaceMarines(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(sortOrder) 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            pageable = PageRequest.of(page, size, sort);
        } else {
            pageable = PageRequest.of(page, size);
        }
        Page<SpaceMarine> pageResult = spaceMarineRepository.findAllWithRelations(pageable);
        return pageResult.getContent();
    }

    public List<SpaceMarine> getSpaceMarinesWithFilters(String nameFilter, String sortBy, String sortOrder, int page, int size) {
        Pageable pageable;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(sortOrder) 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            pageable = PageRequest.of(page, size, sort);
        } else {
            pageable = PageRequest.of(page, size);
        }
        Page<SpaceMarine> pageResult = spaceMarineRepository.findWithNameFilter(nameFilter, pageable);
        return pageResult.getContent();
    }

    public long getSpaceMarinesCount() {
        return spaceMarineRepository.count();
    }

    public long getSpaceMarinesCountWithFilters(String nameFilter) {
        return spaceMarineRepository.countWithNameFilter(nameFilter);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public SpaceMarine updateSpaceMarine(Integer id, SpaceMarine updatedSpaceMarine) {
        logger.info("Обновление space marine: id={}, name={}, health={}", 
            id, updatedSpaceMarine.getName(), updatedSpaceMarine.getHealth());
        
        try {
            // Сначала находим объект, затем блокируем его для обновления
            logger.info("Поиск space marine с ID: {}", id);
            Optional<SpaceMarine> existingSpaceMarineOpt = spaceMarineRepository.findById(id);
            if (!existingSpaceMarineOpt.isPresent()) {
                logger.warn("Space marine не найден для обновления: id={}", id);
                return null;
            }
            
            // Блокируем объект для обновления
            logger.info("Блокировка space marine для обновления: id={}", id);
            SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (spaceMarine == null) {
                logger.warn("Space marine не найден после блокировки: id={}", id);
                return null;
            }
        
            // Проверка уникальности при изменении критических полей
            if (spaceMarine.getChapter() != null && updatedSpaceMarine.getChapter() != null &&
                updatedSpaceMarine.getChapter().getId() != null &&
                (spaceMarine.getHealth() != updatedSpaceMarine.getHealth() ||
                 spaceMarine.getWeaponType() != updatedSpaceMarine.getWeaponType() ||
                 !spaceMarine.getCoordinates().getId().equals(updatedSpaceMarine.getCoordinates().getId()))) {
                logger.info("Проверка уникальности при обновлении: chapterId={}, health={}, weapon={}, coordinatesId={}", 
                    updatedSpaceMarine.getChapter().getId(), updatedSpaceMarine.getHealth(), 
                    updatedSpaceMarine.getWeaponType(), updatedSpaceMarine.getCoordinates().getId());
                List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                        updatedSpaceMarine.getChapter().getId(),
                        updatedSpaceMarine.getHealth(),
                        updatedSpaceMarine.getWeaponType(),
                        updatedSpaceMarine.getCoordinates().getId());
                if (!duplicates.isEmpty() && !duplicates.get(0).getId().equals(id)) {
                    logger.warn("Попытка обновить на дубликат: id={}, chapterId={}, health={}", 
                        id, updatedSpaceMarine.getChapter().getId(), updatedSpaceMarine.getHealth());
                    throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                }
                logger.info("Проверка уникальности при обновлении прошла успешно");
            }
                
                // Handle chapter change
                if (spaceMarine.getChapter() != null && !spaceMarine.getChapter().equals(updatedSpaceMarine.getChapter())) {
                    logger.info("Изменение главы: удаление из старой главы id={}", spaceMarine.getChapter().getId());
                    chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
                }
                
                if (updatedSpaceMarine.getChapter() != null && updatedSpaceMarine.getChapter().getId() != null) {
                    logger.info("Добавление в новую главу: chapterId={}", updatedSpaceMarine.getChapter().getId());
                    Optional<Chapter> newChapter = chapterRepository.findById(updatedSpaceMarine.getChapter().getId());
                    if (newChapter.isPresent()) {
                        spaceMarine.setChapter(newChapter.get());
                        chapterRepository.addMarineToChapter(newChapter.get().getId());
                    }
                } else {
                    logger.info("Удаление из главы");
                    spaceMarine.setChapter(null);
                }
                
                // Update other fields
                logger.info("Обновление полей space marine");
                spaceMarine.setName(updatedSpaceMarine.getName());
                spaceMarine.setCoordinates(updatedSpaceMarine.getCoordinates());
                spaceMarine.setHealth(updatedSpaceMarine.getHealth());
                spaceMarine.setHeartCount(updatedSpaceMarine.getHeartCount());
                spaceMarine.setCategory(updatedSpaceMarine.getCategory());
                spaceMarine.setWeaponType(updatedSpaceMarine.getWeaponType());
                
                // Валидация DTO уже выполнена в контроллере через @Valid (если используется)
                // Уникальность проверена выше с блокировкой
                // Базовые ограничения (NotNull, Min, Max) проверяются на уровне БД
                // Не вызываем validator.validate() здесь, чтобы избежать проблем с блокировками в UniqueFieldsValidator
                
                logger.info("Сохранение обновленного space marine в БД: id={}", id);
                SpaceMarine saved = spaceMarineRepository.save(spaceMarine);
                logger.info("Space marine успешно обновлен: id={}, name={}", saved.getId(), saved.getName());
                return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при обновлении space marine: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }
    
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public SpaceMarine updateSpaceMarineFromDTO(Integer id, SpaceMarineDTO dto) {
        logger.info("Обновление space marine из DTO: id={}, name={}, health={}, coordinatesId={}, chapterId={}", 
            id, dto.name(), dto.health(), dto.coordinatesId(), dto.chapterId());
        
        try {
            // Сначала находим объект, затем блокируем его для обновления
            logger.info("Поиск space marine с ID: {}", id);
            Optional<SpaceMarine> existingSpaceMarineOpt = spaceMarineRepository.findById(id);
            if (!existingSpaceMarineOpt.isPresent()) {
                logger.warn("Space marine не найден для обновления из DTO: id={}", id);
                return null;
            }
            
            // Блокируем объект для обновления
            logger.info("Блокировка space marine для обновления: id={}", id);
            SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (spaceMarine == null) {
                logger.warn("Space marine не найден после блокировки: id={}", id);
                return null;
            }
        
            // Проверка уникальности при изменении критических полей
            if (dto.chapterId() != null && spaceMarine.getHealth() != null && dto.coordinatesId() != null) {
                Long oldChapterId = spaceMarine.getChapter() != null ? spaceMarine.getChapter().getId() : null;
                Long oldCoordinatesId = spaceMarine.getCoordinates() != null ? spaceMarine.getCoordinates().getId() : null;
                
                logger.info("Проверка уникальности при обновлении из DTO: chapterId={}, health={}, coordinatesId={}", 
                    dto.chapterId(), dto.health(), dto.coordinatesId());
                if (!dto.chapterId().equals(oldChapterId) || 
                    !spaceMarine.getHealth().equals(dto.health()) ||
                    !oldCoordinatesId.equals(dto.coordinatesId()) ||
                    (spaceMarine.getWeaponType() != null ? spaceMarine.getWeaponType().name() : null) != 
                    (dto.weaponType() != null ? dto.weaponType() : null)) {
                    // Преобразуем строку weaponType в enum Weapon
                    Weapon weaponTypeEnum = null;
                    if (dto.weaponType() != null && !dto.weaponType().trim().isEmpty()) {
                        try {
                            weaponTypeEnum = Weapon.valueOf(dto.weaponType());
                        } catch (IllegalArgumentException e) {
                            logger.error("Неверный тип оружия при проверке уникальности: {}", dto.weaponType());
                            throw new IllegalArgumentException("Неверный тип оружия: " + dto.weaponType());
                        }
                    }
                    List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                            dto.chapterId(),
                            dto.health(),
                            weaponTypeEnum,
                            dto.coordinatesId());
                    if (!duplicates.isEmpty() && !duplicates.get(0).getId().equals(id)) {
                        logger.warn("Попытка обновить на дубликат из DTO: id={}, chapterId={}, health={}", 
                            id, dto.chapterId(), dto.health());
                        throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                    }
                }
                logger.info("Проверка уникальности при обновлении из DTO прошла успешно");
            }
            
            // Обновляем основные поля
            logger.info("Обновление основных полей space marine");
            spaceMarine.setName(dto.name());
            spaceMarine.setHealth(dto.health());
            spaceMarine.setHeartCount(dto.heartCount());
            
            // Обработка категории
            if (dto.category() != null && !dto.category().trim().isEmpty()) {
                logger.info("Обновление категории: {}", dto.category());
                try {
                    spaceMarine.setCategory(AstartesCategory.valueOf(dto.category()));
                } catch (IllegalArgumentException e) {
                    logger.error("Неверная категория: {}", dto.category());
                    throw new IllegalArgumentException("Неверная категория: " + dto.category());
                }
            } else {
                logger.info("Удаление категории");
                spaceMarine.setCategory(null);
            }
            
            // Обработка оружия
            if (dto.weaponType() != null && !dto.weaponType().trim().isEmpty()) {
                logger.info("Обновление типа оружия: {}", dto.weaponType());
                try {
                    spaceMarine.setWeaponType(Weapon.valueOf(dto.weaponType()));
                } catch (IllegalArgumentException e) {
                    logger.error("Неверный тип оружия: {}", dto.weaponType());
                    throw new IllegalArgumentException("Неверный тип оружия: " + dto.weaponType());
                }
            } else {
                logger.info("Удаление типа оружия");
                spaceMarine.setWeaponType(null);
            }
            
            // Обработка координат (обязательно)
            if (dto.coordinatesId() == null) {
                logger.error("Координаты не указаны при обновлении");
                throw new IllegalArgumentException("Координаты обязательны");
            }
            logger.info("Поиск координат с ID: {}", dto.coordinatesId());
            Optional<Coordinates> coordinates = coordinatesRepository.findById(dto.coordinatesId());
            if (!coordinates.isPresent()) {
                logger.error("Координаты с ID {} не найдены при обновлении", dto.coordinatesId());
                throw new EntityNotFoundException("Coordinates", dto.coordinatesId());
            }
            spaceMarine.setCoordinates(coordinates.get());
            logger.info("Координаты обновлены: id={}, x={}, y={}", 
                coordinates.get().getId(), coordinates.get().getX(), coordinates.get().getY());
            
            // Обработка главы
            Long oldChapterId = spaceMarine.getChapter() != null ? spaceMarine.getChapter().getId() : null;
            
            // Устанавливаем новую главу
            if (dto.chapterId() != null) {
                logger.info("Поиск главы с ID: {}", dto.chapterId());
                Optional<Chapter> chapter = chapterRepository.findById(dto.chapterId());
                if (chapter.isPresent()) {
                    spaceMarine.setChapter(chapter.get());
                    logger.info("Глава обновлена: id={}, name={}", chapter.get().getId(), chapter.get().getName());
                } else {
                    logger.error("Глава с ID {} не найдена при обновлении", dto.chapterId());
                    throw new EntityNotFoundException("Chapter", dto.chapterId());
                }
            } else {
                logger.info("Удаление из главы");
                spaceMarine.setChapter(null);
            }
            
            // Валидация DTO уже выполнена в контроллере через @Valid (если используется)
            // Уникальность проверена выше с блокировкой
            // Базовые ограничения (NotNull, Min, Max) проверяются на уровне БД
            // Не вызываем validator.validate() здесь, чтобы избежать проблем с блокировками в UniqueFieldsValidator
            
            // Сначала обновляем SpaceMarine
            logger.info("Сохранение обновленного space marine в БД: id={}", id);
            SpaceMarine updatedSpaceMarine = spaceMarineRepository.save(spaceMarine);
            logger.info("Space marine успешно сохранен в БД: id={}", updatedSpaceMarine.getId());
            
            // Затем обновляем счетчики в Chapter
            if (oldChapterId != null && (dto.chapterId() == null || !oldChapterId.equals(dto.chapterId()))) {
                logger.info("Удаление марина из старой главы: chapterId={}", oldChapterId);
                chapterRepository.removeMarineFromChapter(oldChapterId);
            }
            if (dto.chapterId() != null && (oldChapterId == null || !oldChapterId.equals(dto.chapterId()))) {
                logger.info("Добавление марина в новую главу: chapterId={}", dto.chapterId());
                chapterRepository.addMarineToChapter(dto.chapterId());
            }
            
            logger.info("Space marine успешно обновлен из DTO: id={}, name={}", 
                updatedSpaceMarine.getId(), updatedSpaceMarine.getName());
            return updatedSpaceMarine;
        } catch (Exception e) {
            logger.error("ОШИБКА при обновлении space marine из DTO: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public boolean deleteSpaceMarine(Integer id) {
        logger.info("Удаление space marine: id={}", id);
        
        try {
            // Сначала находим объект, затем блокируем его для удаления
            logger.info("Поиск space marine с ID: {}", id);
            Optional<SpaceMarine> spaceMarineOpt = spaceMarineRepository.findById(id);
            if (!spaceMarineOpt.isPresent()) {
                logger.warn("Space marine не найден для удаления: id={}", id);
                return false;
            }
            
            // Блокируем объект для удаления
            logger.info("Блокировка space marine для удаления: id={}", id);
            SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (spaceMarine == null) {
                logger.warn("Space marine не найден после блокировки: id={}", id);
                return false;
            }
            
            if (spaceMarine.getChapter() != null) {
                logger.info("Удаление марина из главы: chapterId={}", spaceMarine.getChapter().getId());
                chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
            }
            
            logger.info("Удаление space marine из БД: id={}", id);
            spaceMarineRepository.deleteById(id);
            logger.info("Space marine успешно удален: id={}", id);
            return true;
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении space marine: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    @Transactional
    public boolean deleteSpaceMarine(Integer id, boolean deleteCoordinates, boolean deleteChapter) {
        logger.info("Удаление space marine с опциями: id={}, deleteCoordinates={}, deleteChapter={}", 
            id, deleteCoordinates, deleteChapter);
        
        try {
            Optional<SpaceMarine> spaceMarine = spaceMarineRepository.findById(id);
            if (spaceMarine.isPresent()) {
                SpaceMarine marine = spaceMarine.get();
                logger.info("Space marine найден: id={}, name={}", marine.getId(), marine.getName());
            
            // Сохраняем ID связанных объектов для удаления после удаления марина
            Long coordinatesIdToDelete = null;
            Long chapterIdToDelete = null;
            boolean shouldDecrementChapterCount = false;
            
                // Проверяем координаты
                if (deleteCoordinates && marine.getCoordinates() != null) {
                    Long coordinatesId = marine.getCoordinates().getId();
                    logger.info("Проверка использования координат: coordinatesId={}", coordinatesId);
                    // Проверяем, используется ли эта координата другими маринами
                    long coordinatesUsageCount = spaceMarineRepository.countByCoordinatesId(coordinatesId);
                    logger.info("Координаты используются {} маринами", coordinatesUsageCount);
                    if (coordinatesUsageCount <= 1) {
                        // Если только один марин использует эти координаты, можно удалить
                        coordinatesIdToDelete = coordinatesId;
                        logger.info("Координаты будут удалены: coordinatesId={}", coordinatesId);
                    } else {
                        logger.info("Координаты не будут удалены (используются другими маринами)");
                    }
                }
                
                // Проверяем главу
                if (deleteChapter && marine.getChapter() != null) {
                    Long chapterId = marine.getChapter().getId();
                    logger.info("Проверка использования главы: chapterId={}", chapterId);
                    // Проверяем, используется ли эта глава другими маринами
                    long chapterUsageCount = spaceMarineRepository.countByChapterId(chapterId);
                    logger.info("Глава используется {} маринами", chapterUsageCount);
                    if (chapterUsageCount <= 1) {
                        // Если только один марин использует эту главу, можно удалить
                        chapterIdToDelete = chapterId;
                        logger.info("Глава будет удалена: chapterId={}", chapterId);
                    } else {
                        // Глава используется другими маринами, не удаляем её
                        shouldDecrementChapterCount = true;
                        logger.info("Глава не будет удалена (используется другими маринами)");
                    }
                } else if (marine.getChapter() != null) {
                    // Если не удаляем главу, но марин принадлежит главе, уменьшаем счетчик
                    shouldDecrementChapterCount = true;
                    logger.info("Счетчик маринов в главе будет уменьшен");
                }
                
                // Сначала убираем марина из главы (уменьшаем счетчик)
                if (shouldDecrementChapterCount && marine.getChapter() != null) {
                    logger.info("Удаление марина из главы: chapterId={}", marine.getChapter().getId());
                    chapterRepository.removeMarineFromChapter(marine.getChapter().getId());
                }
                
                // Удаляем самого марина
                logger.info("Удаление space marine из БД: id={}", id);
                spaceMarineRepository.deleteById(id);
                
                // Теперь можем безопасно удалить связанные объекты (если они не используются другими маринами)
                if (coordinatesIdToDelete != null) {
                    logger.info("Удаление координат: coordinatesId={}", coordinatesIdToDelete);
                    coordinatesService.deleteCoordinates(coordinatesIdToDelete);
                }
                
                if (chapterIdToDelete != null) {
                    logger.info("Удаление главы: chapterId={}", chapterIdToDelete);
                    chapterService.deleteChapter(chapterIdToDelete);
                }
                
                logger.info("Space marine успешно удален с опциями: id={}", id);
                return true;
            }
            logger.warn("Space marine не найден для удаления: id={}", id);
            return false;
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении space marine с опциями: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    @Transactional
    public DeleteResponse deleteSpaceMarineWithDetails(Integer id, boolean deleteCoordinates, boolean deleteChapter) {
        Optional<SpaceMarine> spaceMarine = spaceMarineRepository.findById(id);
        if (spaceMarine.isPresent()) {
            SpaceMarine marine = spaceMarine.get();
            
            // Сохраняем ID связанных объектов для удаления после удаления марина
            Long coordinatesIdToDelete = null;
            Long chapterIdToDelete = null;
            boolean shouldDecrementChapterCount = false;
            boolean coordinatesActuallyDeleted = false;
            boolean chapterActuallyDeleted = false;
            boolean coordinatesUsedByOthers = false;
            boolean chapterUsedByOthers = false;
            
            // Проверяем координаты
            if (deleteCoordinates && marine.getCoordinates() != null) {
                Long coordinatesId = marine.getCoordinates().getId();
                // Проверяем, используется ли эта координата другими маринами
                long coordinatesUsageCount = spaceMarineRepository.countByCoordinatesId(coordinatesId);
                if (coordinatesUsageCount <= 1) {
                    // Если только один марин использует эти координаты, можно удалить
                    coordinatesIdToDelete = coordinatesId;
                } else {
                    // Координаты используются другими маринами, не удаляем их
                    coordinatesUsedByOthers = true;
                }
            }
            
            if (deleteChapter && marine.getChapter() != null) {
                Long chapterId = marine.getChapter().getId();
                long chapterUsageCount = spaceMarineRepository.countByChapterId(chapterId);
                if (chapterUsageCount <= 1) {
                    chapterIdToDelete = chapterId;
                } else {
                    chapterUsedByOthers = true;
                    shouldDecrementChapterCount = true;
                }
            } else if (marine.getChapter() != null) {
                shouldDecrementChapterCount = true;
            }
            
            if (shouldDecrementChapterCount && marine.getChapter() != null) {
                chapterRepository.removeMarineFromChapter(marine.getChapter().getId());
            }
            
            spaceMarineRepository.deleteById(id);
            
            if (coordinatesIdToDelete != null) {
                coordinatesActuallyDeleted = coordinatesService.deleteCoordinates(coordinatesIdToDelete);
            }
            
            if (chapterIdToDelete != null) {
                chapterActuallyDeleted = chapterService.deleteChapter(chapterIdToDelete);
            }
            
            StringBuilder message = new StringBuilder("Десантник удален");
            if (coordinatesActuallyDeleted) {
                message.append(", координаты также удалены");
            } else if (deleteCoordinates && coordinatesUsedByOthers) {
                message.append(", координаты НЕ удалены (используются другими маринами)");
            }
            
            if (chapterActuallyDeleted) {
                message.append(", глава также удалена");
            } else if (deleteChapter && chapterUsedByOthers) {
                message.append(", глава НЕ удалена (используется другими маринами)");
            }
            
            return new DeleteResponse(coordinatesActuallyDeleted, chapterActuallyDeleted, message.toString());
        }
        return new DeleteResponse("Десантник не найден");
    }

    public List<SpaceMarine> findSpaceMarinesByNameContaining(String name) {
        return spaceMarineRepository.findByNameContaining(name);
    }

    public List<SpaceMarine> findSpaceMarinesByHealthLessThan(Integer health) {
        return spaceMarineRepository.findByHealthLessThan(health);
    }

    public long countSpaceMarinesByHealthLessThan(Integer health) {
        return spaceMarineRepository.countByHealthLessThan(health);
    }

    public Double getAverageHeartCount() {
        return spaceMarineRepository.getAverageHeartCount();
    }
    
    public RelatedObjectsResponse getRelatedObjects(Integer id) {
        Optional<SpaceMarine> spaceMarine = spaceMarineRepository.findById(id);
        if (!spaceMarine.isPresent()) {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
        
        SpaceMarine marine = spaceMarine.get();
        boolean hasCoordinates = marine.getCoordinates() != null;
        CoordinatesDTO coordinates = hasCoordinates ? spaceMarineMapper.toCoordinatesDTO(marine.getCoordinates()) : null;
        boolean hasChapter = marine.getChapter() != null;
        ChapterDTO chapter = hasChapter ? spaceMarineMapper.toChapterDTO(marine.getChapter()) : null;
        
        return new RelatedObjectsResponse(hasCoordinates, coordinates, hasChapter, chapter);
    }

    @Transactional
    public SpaceMarine removeMarineFromChapter(Integer id) {
        logger.info("Удаление space marine из главы: id={}", id);
        
        try {
            Optional<SpaceMarine> spaceMarineOpt = spaceMarineRepository.findById(id);
            if (!spaceMarineOpt.isPresent()) {
                logger.warn("Space marine не найден: id={}", id);
                return null;
            }
            
            SpaceMarine spaceMarine = spaceMarineOpt.get();
            if (spaceMarine.getChapter() == null) {
                logger.warn("У space marine нет главы для отчисления: id={}", id);
                throw new IllegalArgumentException("У десантника нет ордена для отчисления");
            }
            
            logger.info("Удаление марина из главы: chapterId={}", spaceMarine.getChapter().getId());
            chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
            
            spaceMarine.setChapter(null);
            
            logger.info("Сохранение space marine без главы: id={}", id);
            SpaceMarine saved = spaceMarineRepository.save(spaceMarine);
            logger.info("Space marine успешно удален из главы: id={}, name={}", saved.getId(), saved.getName());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении space marine из главы: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            throw e;
        }
    }
}
