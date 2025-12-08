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
import se.ifmo.ru.back.repository.CoordinatesRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.dto.SpaceMarineDTO;
import se.ifmo.ru.back.entity.Coordinates;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.exception.ValidationException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.CoordinatesService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoordinatesServiceImpl implements CoordinatesService {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatesServiceImpl.class);

    private final CoordinatesRepository coordinatesRepository;
    private final SpaceMarineRepository spaceMarineRepository;
    private final SpaceMarineMapper spaceMarineMapper;
    private final Validator validator;
    private final EntityManager entityManager;

    public CoordinatesServiceImpl(
            CoordinatesRepository coordinatesRepository,
            SpaceMarineRepository spaceMarineRepository,
            SpaceMarineMapper spaceMarineMapper,
            Validator validator,
            EntityManager entityManager) {
        this.coordinatesRepository = coordinatesRepository;
        this.spaceMarineRepository = spaceMarineRepository;
        this.spaceMarineMapper = spaceMarineMapper;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public Coordinates createCoordinates(Coordinates coordinates) {
        logger.info("Создание координат: x={}, y={}", coordinates.getX(), coordinates.getY());
        try {
            // Проверка уникальности с блокировкой для предотвращения race conditions
            logger.info("Проверка уникальности координат: x={}, y={}", coordinates.getX(), coordinates.getY());
            Optional<Coordinates> existing = coordinatesRepository.findByXAndYWithLock(
                    coordinates.getX(), coordinates.getY());
            if (existing.isPresent()) {
                logger.error("Координаты с такими значениями x и y уже существуют: x={}, y={}", 
                    coordinates.getX(), coordinates.getY());
                throw new ValidationException("Координаты с такими значениями x и y уже существуют");
            }
            logger.info("Проверка уникальности прошла успешно");
            
            // Валидация сущности (включая кастомные валидаторы)
            logger.info("Валидация координат: x={}, y={}", coordinates.getX(), coordinates.getY());
            Set<ConstraintViolation<Coordinates>> violations = validator.validate(coordinates);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                logger.error("Ошибка валидации координат: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            logger.info("Валидация координат прошла успешно");
            
            logger.info("Сохранение координат в БД: x={}, y={}", coordinates.getX(), coordinates.getY());
            Coordinates saved = coordinatesRepository.save(coordinates);
            logger.info("Координаты успешно созданы: id={}, x={}, y={}", 
                saved.getId(), saved.getX(), saved.getY());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при создании координат: x={}, y={}", 
                coordinates.getX(), coordinates.getY(), e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    public Optional<Coordinates> getCoordinatesById(Long id) {
        logger.info("Получение координат по ID: id={}", id);
        try {
            Optional<Coordinates> coordinates = coordinatesRepository.findById(id);
            if (coordinates.isPresent()) {
                logger.info("Координаты найдены: id={}, x={}, y={}", 
                    id, coordinates.get().getX(), coordinates.get().getY());
            } else {
                logger.info("Координаты не найдены: id={}", id);
            }
            return coordinates;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении координат по ID: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Coordinates> getAllCoordinates() {
        logger.info("Получение всех координат");
        try {
            List<Coordinates> coordinates = coordinatesRepository.findAll();
            logger.info("Найдено координат: {}", coordinates.size());
            return coordinates;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении всех координат", e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Coordinates> getCoordinates(int page, int size) {
        logger.info("Получение координат с пагинацией: page={}, size={}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Coordinates> pageResult = coordinatesRepository.findAll(pageable);
            logger.info("Найдено координат: {} из {}", pageResult.getContent().size(), pageResult.getTotalElements());
            return pageResult.getContent();
        } catch (Exception e) {
            logger.error("ОШИБКА при получении координат с пагинацией: page={}, size={}", page, size, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Coordinates> getCoordinates(int page, int size, String sortBy, String sortOrder) {
        logger.info("Получение координат с пагинацией и сортировкой: page={}, size={}, sortBy={}, sortOrder={}", 
            page, size, sortBy, sortOrder);
        try {
            Pageable pageable;
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                Sort sort = "desc".equalsIgnoreCase(sortOrder) 
                    ? Sort.by(sortBy).descending() 
                    : Sort.by(sortBy).ascending();
                pageable = PageRequest.of(page, size, sort);
            } else {
                pageable = PageRequest.of(page, size);
            }
            Page<Coordinates> pageResult = coordinatesRepository.findAll(pageable);
            logger.info("Найдено координат: {} из {}", pageResult.getContent().size(), pageResult.getTotalElements());
            return pageResult.getContent();
        } catch (Exception e) {
            logger.error("ОШИБКА при получении координат с пагинацией и сортировкой: page={}, size={}, sortBy={}, sortOrder={}", 
                page, size, sortBy, sortOrder, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public long getCoordinatesCount() {
        logger.info("Получение количества координат");
        try {
            long count = coordinatesRepository.count();
            logger.info("Количество координат: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении количества координат", e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Coordinates updateCoordinates(Long id, Coordinates updatedCoordinates) {
        logger.info("Обновление координат: id={}, x={}, y={}", 
            id, updatedCoordinates.getX(), updatedCoordinates.getY());
        try {
            // Сначала находим объект, затем блокируем его для обновления
            logger.info("Поиск координат для обновления: id={}", id);
            Optional<Coordinates> existingCoordinatesOpt = coordinatesRepository.findById(id);
            if (!existingCoordinatesOpt.isPresent()) {
                logger.warn("Координаты не найдены для обновления: id={}", id);
                return null;
            }
            
            // Блокируем объект для обновления
            logger.info("Блокировка координат для обновления: id={}", id);
            Coordinates coordinates = entityManager.find(Coordinates.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (coordinates == null) {
                logger.warn("Координаты не найдены после блокировки: id={}", id);
                return null;
            }
            
            // Проверяем уникальность новых значений (исключая текущий объект)
            if (!coordinates.getX().equals(updatedCoordinates.getX()) || 
                !java.util.Objects.equals(coordinates.getY(), updatedCoordinates.getY())) {
                logger.info("Проверка уникальности новых значений: x={}, y={}", 
                    updatedCoordinates.getX(), updatedCoordinates.getY());
                Optional<Coordinates> duplicate = coordinatesRepository.findByXAndYWithLock(
                        updatedCoordinates.getX(), updatedCoordinates.getY());
                if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                    logger.error("Координаты с такими значениями x и y уже существуют: x={}, y={}", 
                        updatedCoordinates.getX(), updatedCoordinates.getY());
                    throw new ValidationException("Координаты с такими значениями x и y уже существуют");
                }
                logger.info("Проверка уникальности прошла успешно");
            }
            
            logger.info("Обновление полей координат: id={}", id);
            coordinates.setX(updatedCoordinates.getX());
            coordinates.setY(updatedCoordinates.getY());
            
            // Валидация DTO уже выполнена в контроллере через @Valid
            // Уникальность проверена выше с блокировкой
            // Базовые ограничения (NotNull) проверяются на уровне БД
            // Не вызываем validator.validate() здесь, чтобы избежать проблем с блокировками в UniqueFieldsValidator
            
            logger.info("Сохранение обновленных координат в БД: id={}", id);
            Coordinates saved = coordinatesRepository.save(coordinates);
            logger.info("Координаты успешно обновлены: id={}, x={}, y={}", 
                saved.getId(), saved.getX(), saved.getY());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при обновлении координат: id={}, x={}, y={}", 
                id, updatedCoordinates.getX(), updatedCoordinates.getY(), e);
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
    public boolean deleteCoordinates(Long id) {
        logger.info("Удаление координат: id={}", id);
        try {
            Optional<Coordinates> coordinates = coordinatesRepository.findById(id);
            if (coordinates.isPresent()) {
                logger.info("Координаты найдены для удаления: id={}, x={}, y={}", 
                    id, coordinates.get().getX(), coordinates.get().getY());
                
                // Автоматически удаляем всех маринов, связанных с этими координатами
                logger.info("Проверка связанных маринов для координат: id={}", id);
                long usageCount = spaceMarineRepository.countByCoordinatesId(id);
                if (usageCount > 0) {
                    logger.info("Удаление {} маринов, связанных с координатами: id={}", usageCount, id);
                    spaceMarineRepository.deleteByCoordinatesId(id);
                    logger.info("Марины успешно удалены");
                } else {
                    logger.info("Связанных маринов не найдено");
                }
                
                logger.info("Удаление координат из БД: id={}", id);
                coordinatesRepository.deleteById(id);
                logger.info("Координаты успешно удалены: id={}", id);
                return true;
            } else {
                logger.warn("Координаты не найдены для удаления: id={}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении координат: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }
    
    public RelatedObjectsResponse getRelatedObjects(Long id) {
        logger.info("Получение связанных объектов для координат: id={}", id);
        try {
            // Находим всех SpaceMarines, которые используют эти координаты
            logger.info("Поиск маринов, связанных с координатами: id={}", id);
            List<SpaceMarine> relatedMarines = spaceMarineRepository.findByCoordinatesId(id);
            logger.info("Найдено связанных маринов: {}", relatedMarines.size());
            
            List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                    .map(spaceMarineMapper::toDTO)
                    .collect(Collectors.toList());
            
            logger.info("Успешно получены связанные объекты: количество маринов={}", relatedMarinesDTO.size());
            return new RelatedObjectsResponse(relatedMarinesDTO);
        } catch (Exception e) {
            logger.error("ОШИБКА при получении связанных объектов для координат: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }
}
