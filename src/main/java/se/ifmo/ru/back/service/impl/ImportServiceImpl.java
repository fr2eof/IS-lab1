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

    private final SpaceMarineRepository spaceMarineRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final ChapterRepository chapterRepository;
    private final ImportHistoryRepository importHistoryRepository;
    private final Validator validator;

    public ImportServiceImpl(
            SpaceMarineRepository spaceMarineRepository,
            CoordinatesRepository coordinatesRepository,
            ChapterRepository chapterRepository,
            ImportHistoryRepository importHistoryRepository,
            Validator validator) {
        this.spaceMarineRepository = spaceMarineRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.chapterRepository = chapterRepository;
        this.importHistoryRepository = importHistoryRepository;
        this.validator = validator;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username) {
        return importSpaceMarines(request, username, null);
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username, Long existingHistoryId) {
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

            // Обрабатываем каждый SpaceMarine из запроса
            for (int i = 0; i < request.spaceMarines().size(); i++) {
                SpaceMarineImportDTO importDTO = request.spaceMarines().get(i);
                
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

                    // Валидация вложенных объектов
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

                    // Создаем или находим Coordinates
                    Coordinates coordinates;
                    if (importDTO.coordinates() != null) {
                        CoordinatesDTO coordDTO = importDTO.coordinates();
                        // Для импорта всегда создаем новые координаты
                        coordinates = new Coordinates();
                        coordinates.setX(coordDTO.x());
                        coordinates.setY(coordDTO.y());
                        coordinates = coordinatesRepository.save(coordinates);
                    } else {
                        throw new ValidationException("Координаты обязательны для SpaceMarine");
                    }

                    // Создаем или находим Chapter
                    Chapter chapter = null;
                    boolean isNewChapter = false;
                    if (importDTO.chapter() != null) {
                        ChapterDTO chapterDTO = importDTO.chapter();
                        // Ищем существующую главу по имени
                        Optional<Chapter> existingChapter = chapterRepository.findByName(chapterDTO.name());
                        if (existingChapter.isPresent()) {
                            chapter = existingChapter.get();
                            // Проверяем, не превышен ли лимит маринов в главе
                            if (chapter.getMarinesCount() >= 1000) {
                                throw new ValidationException("Глава " + chapter.getName() + " уже содержит максимальное количество маринов (1000)");
                            }
                        } else {
                            // При создании нового Chapter счетчик должен быть 1, так как мы сразу добавляем марина
                            // И валидация требует минимум 1 марина
                            chapter = new Chapter();
                            chapter.setName(chapterDTO.name());
                            chapter.setMarinesCount(1);
                            chapter = chapterRepository.save(chapter);
                            isNewChapter = true;
                        }
                    }

                    // Создаем SpaceMarine
                    SpaceMarine spaceMarine = new SpaceMarine();
                    spaceMarine.setName(importDTO.name());
                    spaceMarine.setCoordinates(coordinates);
                    spaceMarine.setChapter(chapter);
                    spaceMarine.setHealth(importDTO.health());
                    spaceMarine.setHeartCount(importDTO.heartCount());

                    // Обработка категории
                    if (importDTO.category() != null && !importDTO.category().trim().isEmpty()) {
                        try {
                            spaceMarine.setCategory(AstartesCategory.valueOf(importDTO.category()));
                        } catch (IllegalArgumentException e) {
                            throw new ValidationException("Неверная категория: " + importDTO.category());
                        }
                    }

                    // Обработка оружия
                    if (importDTO.weaponType() != null && !importDTO.weaponType().trim().isEmpty()) {
                        try {
                            spaceMarine.setWeaponType(Weapon.valueOf(importDTO.weaponType()));
                        } catch (IllegalArgumentException e) {
                            throw new ValidationException("Неверный тип оружия: " + importDTO.weaponType());
                        }
                    }

                    // Сохраняем SpaceMarine
                    spaceMarine = spaceMarineRepository.save(spaceMarine);

                    // Обновляем счетчик в Chapter только если это существующая глава
                    // Для новой главы счетчик уже установлен в 1 при создании
                    if (chapter != null && !isNewChapter) {
                        chapterRepository.addMarineToChapter(chapter.getId());
                    }

                    createdMarines.add(spaceMarine);

                } catch (Exception e) {
                    validationErrors.add("Объект #" + (i + 1) + ": " + e.getMessage());
                }
            }

            // Если были ошибки валидации, устанавливаем статус FAILED
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

            // Если не было создано ни одного объекта
            if (createdMarines.isEmpty()) {
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

        } catch (Exception e) {
            // Обновляем статус на FAILED при любой ошибке
            importHistory.setStatus(ImportStatus.FAILED);
            if (importHistory.getErrorMessage() == null) {
                importHistory.setErrorMessage(e.getMessage());
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
        
        CoordinatesDTO coordinatesDTO = null;
        if (json.containsKey("coordinates")) {
            jakarta.json.JsonObject coordsJson = json.getJsonObject("coordinates");
            Float x = coordsJson.containsKey("x") ? (float) coordsJson.getJsonNumber("x").doubleValue() : null;
            Double y = coordsJson.containsKey("y") && !coordsJson.isNull("y") 
                    ? coordsJson.getJsonNumber("y").doubleValue() 
                    : null;
            coordinatesDTO = new CoordinatesDTO(null, x, y);
        }
        
        ChapterDTO chapterDTO = null;
        if (json.containsKey("chapter")) {
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
                coordinatesDTO,
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
