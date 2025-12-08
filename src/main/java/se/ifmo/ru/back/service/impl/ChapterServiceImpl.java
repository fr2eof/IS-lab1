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
import se.ifmo.ru.back.repository.ChapterRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.dto.SpaceMarineDTO;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.exception.ValidationException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.ChapterService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl implements ChapterService {

    private static final Logger logger = LoggerFactory.getLogger(ChapterServiceImpl.class);

    private final ChapterRepository chapterRepository;
    private final SpaceMarineRepository spaceMarineRepository;
    private final SpaceMarineMapper spaceMarineMapper;
    private final Validator validator;
     private final EntityManager entityManager;

    public ChapterServiceImpl(
            ChapterRepository chapterRepository,
            SpaceMarineRepository spaceMarineRepository,
            SpaceMarineMapper spaceMarineMapper,
            Validator validator,
            EntityManager entityManager) {
        this.chapterRepository = chapterRepository;
        this.spaceMarineRepository = spaceMarineRepository;
        this.spaceMarineMapper = spaceMarineMapper;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public Chapter createChapter(Chapter chapter) {
        logger.info("Создание главы: name={}, marinesCount={}", chapter.getName(), chapter.getMarinesCount());
        try {
            // Проверка уникальности с блокировкой для предотвращения race conditions
            logger.info("Проверка уникальности главы: name={}, marinesCount={}", chapter.getName(), chapter.getMarinesCount());
            Optional<Chapter> existing = chapterRepository.findByNameAndMarinesCountWithLock(
                    chapter.getName(), chapter.getMarinesCount());
            if (existing.isPresent()) {
                logger.error("Глава с таким именем и количеством маринов уже существует: name={}, marinesCount={}", 
                    chapter.getName(), chapter.getMarinesCount());
                throw new ValidationException("Глава с таким именем и количеством маринов уже существует");
            }
            logger.info("Проверка уникальности прошла успешно");
            
            // Валидация сущности (включая кастомные валидаторы)
            logger.info("Валидация главы: name={}", chapter.getName());
            Set<ConstraintViolation<Chapter>> violations = validator.validate(chapter);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                logger.error("Ошибка валидации главы: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            logger.info("Валидация главы прошла успешно");
            
            logger.info("Сохранение главы в БД: name={}, marinesCount={}", chapter.getName(), chapter.getMarinesCount());
            Chapter saved = chapterRepository.save(chapter);
            logger.info("Глава успешно создана: id={}, name={}, marinesCount={}", 
                saved.getId(), saved.getName(), saved.getMarinesCount());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при создании главы: name={}, marinesCount={}", 
                chapter.getName(), chapter.getMarinesCount(), e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    public Optional<Chapter> getChapterById(Long id) {
        logger.info("Получение главы по ID: id={}", id);
        try {
            Optional<Chapter> chapter = chapterRepository.findById(id);
            if (chapter.isPresent()) {
                logger.info("Глава найдена: id={}, name={}, marinesCount={}", 
                    id, chapter.get().getName(), chapter.get().getMarinesCount());
            } else {
                logger.info("Глава не найдена: id={}", id);
            }
            return chapter;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении главы по ID: id={}", id, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Chapter> getAllChapters() {
        logger.info("Получение всех глав");
        try {
            List<Chapter> chapters = chapterRepository.findAll();
            logger.info("Найдено глав: {}", chapters.size());
            return chapters;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении всех глав", e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Chapter> getChapters(int page, int size) {
        logger.info("Получение глав с пагинацией: page={}, size={}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Chapter> pageResult = chapterRepository.findAll(pageable);
            logger.info("Найдено глав: {} из {}", pageResult.getContent().size(), pageResult.getTotalElements());
            return pageResult.getContent();
        } catch (Exception e) {
            logger.error("ОШИБКА при получении глав с пагинацией: page={}, size={}", page, size, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public List<Chapter> getChapters(int page, int size, String sortBy, String sortOrder) {
        logger.info("Получение глав с пагинацией и сортировкой: page={}, size={}, sortBy={}, sortOrder={}", 
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
            Page<Chapter> pageResult = chapterRepository.findAll(pageable);
            logger.info("Найдено глав: {} из {}", pageResult.getContent().size(), pageResult.getTotalElements());
            return pageResult.getContent();
        } catch (Exception e) {
            logger.error("ОШИБКА при получении глав с пагинацией и сортировкой: page={}, size={}, sortBy={}, sortOrder={}", 
                page, size, sortBy, sortOrder, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public long getChaptersCount() {
        logger.info("Получение количества глав");
        try {
            long count = chapterRepository.count();
            logger.info("Количество глав: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении количества глав", e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public Optional<Chapter> getChapterByName(String name) {
        logger.info("Получение главы по имени: name={}", name);
        try {
            Optional<Chapter> chapter = chapterRepository.findByName(name);
            if (chapter.isPresent()) {
                logger.info("Глава найдена: id={}, name={}, marinesCount={}", 
                    chapter.get().getId(), name, chapter.get().getMarinesCount());
            } else {
                logger.info("Глава не найдена: name={}", name);
            }
            return chapter;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении главы по имени: name={}", name, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Chapter updateChapter(Long id, Chapter updatedChapter) {
        logger.info("Обновление главы: id={}, name={}, marinesCount={}", 
            id, updatedChapter.getName(), updatedChapter.getMarinesCount());
        try {
            // Сначала находим объект, затем блокируем его для обновления
            logger.info("Поиск главы для обновления: id={}", id);
            Optional<Chapter> existingChapterOpt = chapterRepository.findById(id);
            if (!existingChapterOpt.isPresent()) {
                logger.warn("Глава не найдена для обновления: id={}", id);
                return null;
            }
            
            // Блокируем объект для обновления
            logger.info("Блокировка главы для обновления: id={}", id);
            Chapter chapter = entityManager.find(Chapter.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (chapter == null) {
                logger.warn("Глава не найдена после блокировки: id={}", id);
                return null;
            }
            
            // Проверяем уникальность новых значений (исключая текущий объект)
            if (!chapter.getName().equals(updatedChapter.getName()) || 
                chapter.getMarinesCount() != updatedChapter.getMarinesCount()) {
                logger.info("Проверка уникальности новых значений: name={}, marinesCount={}", 
                    updatedChapter.getName(), updatedChapter.getMarinesCount());
                Optional<Chapter> duplicate = chapterRepository.findByNameAndMarinesCountWithLock(
                        updatedChapter.getName(), updatedChapter.getMarinesCount());
                if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                    logger.error("Глава с таким именем и количеством маринов уже существует: name={}, marinesCount={}", 
                        updatedChapter.getName(), updatedChapter.getMarinesCount());
                    throw new ValidationException("Глава с таким именем и количеством маринов уже существует");
                }
                logger.info("Проверка уникальности прошла успешно");
            }
            
            logger.info("Обновление полей главы: id={}", id);
            chapter.setName(updatedChapter.getName());
            chapter.setMarinesCount(updatedChapter.getMarinesCount());
            
            // Валидация DTO уже выполнена в контроллере через @Valid
            // Уникальность проверена выше с блокировкой
            // Базовые ограничения (NotNull, Min, Max) проверяются на уровне БД
            // Не вызываем validator.validate() здесь, чтобы избежать проблем с блокировками в UniqueFieldsValidator
            
            logger.info("Сохранение обновленной главы в БД: id={}", id);
            Chapter saved = chapterRepository.save(chapter);
            logger.info("Глава успешно обновлена: id={}, name={}, marinesCount={}", 
                saved.getId(), saved.getName(), saved.getMarinesCount());
            return saved;
        } catch (Exception e) {
            logger.error("ОШИБКА при обновлении главы: id={}, name={}, marinesCount={}", 
                id, updatedChapter.getName(), updatedChapter.getMarinesCount(), e);
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
    public boolean deleteChapter(Long id) {
        logger.info("Удаление главы: id={}", id);
        try {
            Optional<Chapter> chapter = chapterRepository.findById(id);
            if (chapter.isPresent()) {
                logger.info("Глава найдена для удаления: id={}, name={}, marinesCount={}", 
                    id, chapter.get().getName(), chapter.get().getMarinesCount());
                
                // Автоматически удаляем всех маринов, связанных с этой главой
                logger.info("Проверка связанных маринов для главы: id={}", id);
                long usageCount = spaceMarineRepository.countByChapterId(id);
                if (usageCount > 0) {
                    logger.info("Удаление {} маринов, связанных с главой: id={}", usageCount, id);
                    spaceMarineRepository.deleteByChapterId(id);
                    logger.info("Марины успешно удалены");
                } else {
                    logger.info("Связанных маринов не найдено");
                }
                
                logger.info("Удаление главы из БД: id={}", id);
                chapterRepository.deleteById(id);
                logger.info("Глава успешно удалена: id={}", id);
                return true;
            } else {
                logger.warn("Глава не найдена для удаления: id={}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении главы: id={}", id, e);
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
    public boolean removeMarineFromChapter(Long chapterId) {
        logger.info("Удаление марина из главы: chapterId={}", chapterId);
        try {
            Optional<Chapter> chapter = chapterRepository.findById(chapterId);
            if (chapter.isPresent() && chapter.get().getMarinesCount() > 0) {
                logger.info("Глава найдена: id={}, текущее количество маринов={}", 
                    chapterId, chapter.get().getMarinesCount());
                int updated = chapterRepository.removeMarineFromChapter(chapterId);
                if (updated > 0) {
                    logger.info("Марин успешно удален из главы: chapterId={}, обновлено записей={}", chapterId, updated);
                } else {
                    logger.warn("Не удалось удалить марина из главы: chapterId={}", chapterId);
                }
                return updated > 0;
            } else {
                logger.warn("Глава не найдена или количество маринов равно 0: chapterId={}", chapterId);
                return false;
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении марина из главы: chapterId={}", chapterId, e);
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
        logger.info("Получение связанных объектов для главы: id={}", id);
        try {
            // Находим всех SpaceMarines, которые используют эту главу
            logger.info("Поиск маринов, связанных с главой: id={}", id);
            if (spaceMarineRepository == null) {
                logger.error("SpaceMarineRepository не инициализирован");
                throw new IllegalStateException("SpaceMarineRepository не инициализирован");
            }
            List<SpaceMarine> relatedMarines = spaceMarineRepository.findByChapterId(id);
            logger.info("Найдено связанных маринов: {}", relatedMarines.size());
            
            List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                    .map(spaceMarineMapper::toDTO)
                    .collect(Collectors.toList());
            
            logger.info("Успешно получены связанные объекты: количество маринов={}", relatedMarinesDTO.size());
            return new RelatedObjectsResponse(relatedMarinesDTO);
        } catch (Exception e) {
            logger.error("ОШИБКА при получении связанных объектов для главы: id={}", id, e);
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
