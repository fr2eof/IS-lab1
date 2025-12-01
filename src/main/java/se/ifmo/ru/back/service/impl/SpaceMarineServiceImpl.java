package se.ifmo.ru.back.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceMarineServiceImpl implements SpaceMarineService {

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
        // Проверка уникальности с блокировкой для предотвращения race conditions
        if (spaceMarine.getChapter() != null && spaceMarine.getChapter().getId() != null && 
            spaceMarine.getHealth() != null && spaceMarine.getCoordinates() != null) {
            List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                    spaceMarine.getChapter().getId(),
                    spaceMarine.getHealth(),
                    spaceMarine.getWeaponType() != null ? spaceMarine.getWeaponType().name() : null,
                    spaceMarine.getCoordinates().getId());
            if (!duplicates.isEmpty()) {
                throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
            }
        }
        
        // Валидация сущности (включая кастомные валидаторы)
        Set<ConstraintViolation<SpaceMarine>> violations = validator.validate(spaceMarine);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errorMessage);
        }
        
        if (spaceMarine.getChapter() != null && spaceMarine.getChapter().getId() != null) {
            Optional<Chapter> existingChapter = chapterRepository.findById(spaceMarine.getChapter().getId());
            if (existingChapter.isPresent()) {
                spaceMarine.setChapter(existingChapter.get());
                chapterRepository.addMarineToChapter(existingChapter.get().getId());
            }
        }
        return spaceMarineRepository.save(spaceMarine);
    }
    
    @Transactional
    public SpaceMarine createSpaceMarineFromDTO(SpaceMarineDTO dto) {
        SpaceMarine spaceMarine = new SpaceMarine();
        spaceMarine.setName(dto.name());
        spaceMarine.setHealth(dto.health());
        spaceMarine.setHeartCount(dto.heartCount());
        
        // Обработка категории
        if (dto.category() != null && !dto.category().trim().isEmpty()) {
            try {
                spaceMarine.setCategory(AstartesCategory.valueOf(dto.category()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверная категория: " + dto.category());
            }
        }
        
        // Обработка оружия
        if (dto.weaponType() != null && !dto.weaponType().trim().isEmpty()) {
            try {
                spaceMarine.setWeaponType(Weapon.valueOf(dto.weaponType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверный тип оружия: " + dto.weaponType());
            }
        }
        
        // Обработка координат (обязательно)
        if (dto.coordinatesId() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        Optional<Coordinates> coordinates = coordinatesRepository.findById(dto.coordinatesId());
        if (!coordinates.isPresent()) {
            throw new EntityNotFoundException("Coordinates", dto.coordinatesId());
        }
        spaceMarine.setCoordinates(coordinates.get());
        
        // Обработка главы (необязательно)
        if (dto.chapterId() != null) {
            Optional<Chapter> chapter = chapterRepository.findById(dto.chapterId());
            if (chapter.isPresent()) {
                spaceMarine.setChapter(chapter.get());
            } else {
                throw new EntityNotFoundException("Chapter", dto.chapterId());
            }
        }
        
        // Валидация сущности (включая кастомные валидаторы)
        Set<ConstraintViolation<SpaceMarine>> violations = validator.validate(spaceMarine);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errorMessage);
        }
        
        // Сначала сохраняем SpaceMarine
        SpaceMarine savedSpaceMarine = spaceMarineRepository.save(spaceMarine);
        
        // Затем обновляем счетчик в Chapter
        if (dto.chapterId() != null) {
            chapterRepository.addMarineToChapter(dto.chapterId());
        }
        
        return savedSpaceMarine;
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
        // Сначала находим объект, затем блокируем его для обновления
        Optional<SpaceMarine> existingSpaceMarineOpt = spaceMarineRepository.findById(id);
        if (!existingSpaceMarineOpt.isPresent()) {
            return null;
        }
        
        // Блокируем объект для обновления
        SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (spaceMarine == null) {
            return null;
        }
        
        // Проверка уникальности при изменении критических полей
        if (spaceMarine.getChapter() != null && updatedSpaceMarine.getChapter() != null &&
            updatedSpaceMarine.getChapter().getId() != null &&
            (spaceMarine.getHealth() != updatedSpaceMarine.getHealth() ||
             spaceMarine.getWeaponType() != updatedSpaceMarine.getWeaponType() ||
             !spaceMarine.getCoordinates().getId().equals(updatedSpaceMarine.getCoordinates().getId()))) {
            List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                    updatedSpaceMarine.getChapter().getId(),
                    updatedSpaceMarine.getHealth(),
                    updatedSpaceMarine.getWeaponType() != null ? updatedSpaceMarine.getWeaponType().name() : null,
                    updatedSpaceMarine.getCoordinates().getId());
            if (!duplicates.isEmpty() && !duplicates.get(0).getId().equals(id)) {
                throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
            }
        }
            
            // Handle chapter change
            if (spaceMarine.getChapter() != null && !spaceMarine.getChapter().equals(updatedSpaceMarine.getChapter())) {
                chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
            }
            
            if (updatedSpaceMarine.getChapter() != null && updatedSpaceMarine.getChapter().getId() != null) {
                Optional<Chapter> newChapter = chapterRepository.findById(updatedSpaceMarine.getChapter().getId());
                if (newChapter.isPresent()) {
                    spaceMarine.setChapter(newChapter.get());
                    chapterRepository.addMarineToChapter(newChapter.get().getId());
                }
            } else {
                spaceMarine.setChapter(null);
            }
            
            // Update other fields
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
            
            return spaceMarineRepository.save(spaceMarine);
    }
    
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public SpaceMarine updateSpaceMarineFromDTO(Integer id, SpaceMarineDTO dto) {
        // Сначала находим объект, затем блокируем его для обновления
        Optional<SpaceMarine> existingSpaceMarineOpt = spaceMarineRepository.findById(id);
        if (!existingSpaceMarineOpt.isPresent()) {
            return null;
        }
        
        // Блокируем объект для обновления
        SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (spaceMarine == null) {
            return null;
        }
        
        // Проверка уникальности при изменении критических полей
        if (dto.chapterId() != null && spaceMarine.getHealth() != null && dto.coordinatesId() != null) {
            Long oldChapterId = spaceMarine.getChapter() != null ? spaceMarine.getChapter().getId() : null;
            Long oldCoordinatesId = spaceMarine.getCoordinates() != null ? spaceMarine.getCoordinates().getId() : null;
            
            if (!dto.chapterId().equals(oldChapterId) || 
                !spaceMarine.getHealth().equals(dto.health()) ||
                !oldCoordinatesId.equals(dto.coordinatesId()) ||
                (spaceMarine.getWeaponType() != null ? spaceMarine.getWeaponType().name() : null) != 
                (dto.weaponType() != null ? dto.weaponType() : null)) {
                List<SpaceMarine> duplicates = spaceMarineRepository.findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
                        dto.chapterId(),
                        dto.health(),
                        dto.weaponType(),
                        dto.coordinatesId());
                if (!duplicates.isEmpty() && !duplicates.get(0).getId().equals(id)) {
                    throw new ValidationException("Десантник с таким здоровьем, оружием и координатами уже существует в этой главе");
                }
            }
        }
        
        // Обновляем основные поля
        spaceMarine.setName(dto.name());
        spaceMarine.setHealth(dto.health());
        spaceMarine.setHeartCount(dto.heartCount());
        
        // Обработка категории
        if (dto.category() != null && !dto.category().trim().isEmpty()) {
            try {
                spaceMarine.setCategory(AstartesCategory.valueOf(dto.category()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверная категория: " + dto.category());
            }
        } else {
            spaceMarine.setCategory(null);
        }
        
        // Обработка оружия
        if (dto.weaponType() != null && !dto.weaponType().trim().isEmpty()) {
            try {
                spaceMarine.setWeaponType(Weapon.valueOf(dto.weaponType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверный тип оружия: " + dto.weaponType());
            }
        } else {
            spaceMarine.setWeaponType(null);
        }
        
        // Обработка координат (обязательно)
        if (dto.coordinatesId() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        Optional<Coordinates> coordinates = coordinatesRepository.findById(dto.coordinatesId());
        if (!coordinates.isPresent()) {
            throw new EntityNotFoundException("Coordinates", dto.coordinatesId());
        }
        spaceMarine.setCoordinates(coordinates.get());
        
        // Обработка главы
        Long oldChapterId = spaceMarine.getChapter() != null ? spaceMarine.getChapter().getId() : null;
        
        // Устанавливаем новую главу
        if (dto.chapterId() != null) {
            Optional<Chapter> chapter = chapterRepository.findById(dto.chapterId());
            if (chapter.isPresent()) {
                spaceMarine.setChapter(chapter.get());
            } else {
                throw new EntityNotFoundException("Chapter", dto.chapterId());
            }
        } else {
            spaceMarine.setChapter(null);
        }
        
        // Валидация DTO уже выполнена в контроллере через @Valid (если используется)
        // Уникальность проверена выше с блокировкой
        // Базовые ограничения (NotNull, Min, Max) проверяются на уровне БД
        // Не вызываем validator.validate() здесь, чтобы избежать проблем с блокировками в UniqueFieldsValidator
        
        // Сначала обновляем SpaceMarine
        SpaceMarine updatedSpaceMarine = spaceMarineRepository.save(spaceMarine);
        
        // Затем обновляем счетчики в Chapter
        if (oldChapterId != null && (dto.chapterId() == null || !oldChapterId.equals(dto.chapterId()))) {
            chapterRepository.removeMarineFromChapter(oldChapterId);
        }
        if (dto.chapterId() != null && (oldChapterId == null || !oldChapterId.equals(dto.chapterId()))) {
            chapterRepository.addMarineToChapter(dto.chapterId());
        }
        
        return updatedSpaceMarine;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public boolean deleteSpaceMarine(Integer id) {
        // Сначала находим объект, затем блокируем его для удаления
        Optional<SpaceMarine> spaceMarineOpt = spaceMarineRepository.findById(id);
        if (!spaceMarineOpt.isPresent()) {
            return false;
        }
        
        // Блокируем объект для удаления
        SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (spaceMarine == null) {
            return false;
        }
        
        if (spaceMarine.getChapter() != null) {
            chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
        }
        spaceMarineRepository.deleteById(id);
        return true;
    }

    @Transactional
    public boolean deleteSpaceMarine(Integer id, boolean deleteCoordinates, boolean deleteChapter) {
        Optional<SpaceMarine> spaceMarine = spaceMarineRepository.findById(id);
        if (spaceMarine.isPresent()) {
            SpaceMarine marine = spaceMarine.get();
            
            // Сохраняем ID связанных объектов для удаления после удаления марина
            Long coordinatesIdToDelete = null;
            Long chapterIdToDelete = null;
            boolean shouldDecrementChapterCount = false;
            
            // Проверяем координаты
            if (deleteCoordinates && marine.getCoordinates() != null) {
                Long coordinatesId = marine.getCoordinates().getId();
                // Проверяем, используется ли эта координата другими маринами
                long coordinatesUsageCount = spaceMarineRepository.countByCoordinatesId(coordinatesId);
                if (coordinatesUsageCount <= 1) {
                    // Если только один марин использует эти координаты, можно удалить
                    coordinatesIdToDelete = coordinatesId;
                }
                // Если координаты используются другими маринами, не удаляем их
                // (так как coordinates обязательны для маринов)
            }
            
            // Проверяем главу
            if (deleteChapter && marine.getChapter() != null) {
                Long chapterId = marine.getChapter().getId();
                // Проверяем, используется ли эта глава другими маринами
                long chapterUsageCount = spaceMarineRepository.countByChapterId(chapterId);
                if (chapterUsageCount <= 1) {
                    // Если только один марин использует эту главу, можно удалить
                    chapterIdToDelete = chapterId;
                } else {
                    // Глава используется другими маринами, не удаляем её
                    shouldDecrementChapterCount = true;
                }
            } else if (marine.getChapter() != null) {
                // Если не удаляем главу, но марин принадлежит главе, уменьшаем счетчик
                shouldDecrementChapterCount = true;
            }
            
            // Сначала убираем марина из главы (уменьшаем счетчик)
            if (shouldDecrementChapterCount && marine.getChapter() != null) {
                chapterRepository.removeMarineFromChapter(marine.getChapter().getId());
            }
            
            // Удаляем самого марина
            spaceMarineRepository.deleteById(id);
            
            // Теперь можем безопасно удалить связанные объекты (если они не используются другими маринами)
            if (coordinatesIdToDelete != null) {
                coordinatesService.deleteCoordinates(coordinatesIdToDelete);
            }
            
            if (chapterIdToDelete != null) {
                chapterService.deleteChapter(chapterIdToDelete);
            }
            
            return true;
        }
        return false;
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
        Optional<SpaceMarine> spaceMarineOpt = spaceMarineRepository.findById(id);
        if (!spaceMarineOpt.isPresent()) {
            return null;
        }
        
        SpaceMarine spaceMarine = spaceMarineOpt.get();
        if (spaceMarine.getChapter() == null) {
            throw new IllegalArgumentException("У десантника нет ордена для отчисления");
        }
        
        chapterRepository.removeMarineFromChapter(spaceMarine.getChapter().getId());
        
        spaceMarine.setChapter(null);
        
        return spaceMarineRepository.save(spaceMarine);
    }
}
