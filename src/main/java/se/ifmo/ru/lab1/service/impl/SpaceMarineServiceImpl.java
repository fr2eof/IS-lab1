package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.SpaceMarineDAO;
import se.ifmo.ru.lab1.dao.ChapterDAO;
import se.ifmo.ru.lab1.dao.CoordinatesDAO;
import se.ifmo.ru.lab1.entity.SpaceMarine;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.entity.Coordinates;
import se.ifmo.ru.lab1.entity.AstartesCategory;
import se.ifmo.ru.lab1.entity.Weapon;
import se.ifmo.ru.lab1.dto.SpaceMarineDTO;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;
import se.ifmo.ru.lab1.dto.ChapterDTO;
import se.ifmo.ru.lab1.dto.CoordinatesDTO;
import se.ifmo.ru.lab1.dto.DeleteResponse;
import se.ifmo.ru.lab1.service.SpaceMarineService;
import se.ifmo.ru.lab1.service.ChapterService;
import se.ifmo.ru.lab1.service.CoordinatesService;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SpaceMarineServiceImpl implements SpaceMarineService {

    @Inject
    private SpaceMarineDAO spaceMarineDAO;

    @Inject
    private ChapterDAO chapterDAO;
    
    @Inject
    private CoordinatesDAO coordinatesDAO;
    
    @Inject
    private ChapterService chapterService;
    
    @Inject
    private CoordinatesService coordinatesService;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @Transactional
    public SpaceMarine createSpaceMarine(SpaceMarine spaceMarine) {
        if (spaceMarine.getChapter() != null && spaceMarine.getChapter().getId() != null) {
            Optional<Chapter> existingChapter = chapterDAO.findById(spaceMarine.getChapter().getId());
            if (existingChapter.isPresent()) {
                spaceMarine.setChapter(existingChapter.get());
                chapterDAO.addMarineToChapter(existingChapter.get().getId());
            }
        }
        return spaceMarineDAO.save(spaceMarine);
    }
    
    @Transactional
    public SpaceMarine createSpaceMarineFromDTO(SpaceMarineDTO dto) {
        SpaceMarine spaceMarine = new SpaceMarine();
        spaceMarine.setName(dto.getName());
        spaceMarine.setHealth(dto.getHealth());
        spaceMarine.setHeartCount(dto.getHeartCount());
        
        // Обработка категории
        if (dto.getCategory() != null && !dto.getCategory().trim().isEmpty()) {
            try {
                spaceMarine.setCategory(AstartesCategory.valueOf(dto.getCategory()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверная категория: " + dto.getCategory());
            }
        }
        
        // Обработка оружия
        if (dto.getWeaponType() != null && !dto.getWeaponType().trim().isEmpty()) {
            try {
                spaceMarine.setWeaponType(Weapon.valueOf(dto.getWeaponType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверный тип оружия: " + dto.getWeaponType());
            }
        }
        
        // Обработка координат (обязательно)
        if (dto.getCoordinatesId() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        Optional<Coordinates> coordinates = coordinatesDAO.findById(dto.getCoordinatesId());
        if (!coordinates.isPresent()) {
            throw new IllegalArgumentException("Координаты с ID " + dto.getCoordinatesId() + " не найдены");
        }
        spaceMarine.setCoordinates(coordinates.get());
        
        // Обработка главы (необязательно)
        if (dto.getChapterId() != null) {
            Optional<Chapter> chapter = chapterDAO.findById(dto.getChapterId());
            if (chapter.isPresent()) {
                spaceMarine.setChapter(chapter.get());
            } else {
                throw new IllegalArgumentException("Глава с ID " + dto.getChapterId() + " не найдена");
            }
        }
        
        // Сначала сохраняем SpaceMarine
        SpaceMarine savedSpaceMarine = spaceMarineDAO.save(spaceMarine);
        
        // Затем обновляем счетчик в Chapter
        if (dto.getChapterId() != null) {
            chapterDAO.addMarineToChapter(dto.getChapterId());
        }
        
        return savedSpaceMarine;
    }

    public Optional<SpaceMarine> getSpaceMarineById(Integer id) {
        return spaceMarineDAO.findById(id);
    }

    public List<SpaceMarine> getAllSpaceMarines() {
        return spaceMarineDAO.findAll();
    }

    public List<SpaceMarine> getSpaceMarines(int page, int size) {
        return spaceMarineDAO.findAll(page, size);
    }

    public List<SpaceMarine> getSpaceMarines(int page, int size, String sortBy, String sortOrder) {
        return spaceMarineDAO.findAll(page, size, sortBy, sortOrder);
    }

    public List<SpaceMarine> getSpaceMarinesWithFilters(String nameFilter, String sortBy, String sortOrder, int page, int size) {
        return spaceMarineDAO.findWithFilters(nameFilter, sortBy, sortOrder, page, size);
    }

    public long getSpaceMarinesCount() {
        return spaceMarineDAO.count();
    }

    public long getSpaceMarinesCountWithFilters(String nameFilter) {
        return spaceMarineDAO.countWithFilters(nameFilter);
    }

    @Transactional
    public SpaceMarine updateSpaceMarine(Integer id, SpaceMarine updatedSpaceMarine) {
        Optional<SpaceMarine> existingSpaceMarine = spaceMarineDAO.findById(id);
        if (existingSpaceMarine.isPresent()) {
            SpaceMarine spaceMarine = existingSpaceMarine.get();
            
            // Handle chapter change
            if (spaceMarine.getChapter() != null && !spaceMarine.getChapter().equals(updatedSpaceMarine.getChapter())) {
                chapterDAO.removeMarineFromChapter(spaceMarine.getChapter().getId());
            }
            
            if (updatedSpaceMarine.getChapter() != null && updatedSpaceMarine.getChapter().getId() != null) {
                Optional<Chapter> newChapter = chapterDAO.findById(updatedSpaceMarine.getChapter().getId());
                if (newChapter.isPresent()) {
                    spaceMarine.setChapter(newChapter.get());
                    chapterDAO.addMarineToChapter(newChapter.get().getId());
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
            
            return spaceMarineDAO.update(spaceMarine);
        }
        return null;
    }
    
    @Transactional
    public SpaceMarine updateSpaceMarineFromDTO(Integer id, SpaceMarineDTO dto) {
        Optional<SpaceMarine> existingSpaceMarine = spaceMarineDAO.findById(id);
        if (!existingSpaceMarine.isPresent()) {
            return null;
        }
        
        SpaceMarine spaceMarine = existingSpaceMarine.get();
        
        // Обновляем основные поля
        spaceMarine.setName(dto.getName());
        spaceMarine.setHealth(dto.getHealth());
        spaceMarine.setHeartCount(dto.getHeartCount());
        
        // Обработка категории
        if (dto.getCategory() != null && !dto.getCategory().trim().isEmpty()) {
            try {
                spaceMarine.setCategory(AstartesCategory.valueOf(dto.getCategory()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверная категория: " + dto.getCategory());
            }
        } else {
            spaceMarine.setCategory(null);
        }
        
        // Обработка оружия
        if (dto.getWeaponType() != null && !dto.getWeaponType().trim().isEmpty()) {
            try {
                spaceMarine.setWeaponType(Weapon.valueOf(dto.getWeaponType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неверный тип оружия: " + dto.getWeaponType());
            }
        } else {
            spaceMarine.setWeaponType(null);
        }
        
        // Обработка координат (обязательно)
        if (dto.getCoordinatesId() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        Optional<Coordinates> coordinates = coordinatesDAO.findById(dto.getCoordinatesId());
        if (!coordinates.isPresent()) {
            throw new IllegalArgumentException("Координаты с ID " + dto.getCoordinatesId() + " не найдены");
        }
        spaceMarine.setCoordinates(coordinates.get());
        
        // Обработка главы
        Long oldChapterId = spaceMarine.getChapter() != null ? spaceMarine.getChapter().getId() : null;
        
        // Устанавливаем новую главу
        if (dto.getChapterId() != null) {
            Optional<Chapter> chapter = chapterDAO.findById(dto.getChapterId());
            if (chapter.isPresent()) {
                spaceMarine.setChapter(chapter.get());
            } else {
                throw new IllegalArgumentException("Глава с ID " + dto.getChapterId() + " не найдена");
            }
        } else {
            spaceMarine.setChapter(null);
        }
        
        // Сначала обновляем SpaceMarine
        SpaceMarine updatedSpaceMarine = spaceMarineDAO.update(spaceMarine);
        
        // Затем обновляем счетчики в Chapter
        if (oldChapterId != null && (dto.getChapterId() == null || !oldChapterId.equals(dto.getChapterId()))) {
            chapterDAO.removeMarineFromChapter(oldChapterId);
        }
        if (dto.getChapterId() != null && (oldChapterId == null || !oldChapterId.equals(dto.getChapterId()))) {
            chapterDAO.addMarineToChapter(dto.getChapterId());
        }
        
        return updatedSpaceMarine;
    }

    @Transactional
    public boolean deleteSpaceMarine(Integer id) {
        Optional<SpaceMarine> spaceMarine = spaceMarineDAO.findById(id);
        if (spaceMarine.isPresent()) {
            if (spaceMarine.get().getChapter() != null) {
                chapterDAO.removeMarineFromChapter(spaceMarine.get().getChapter().getId());
            }
            spaceMarineDAO.delete(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteSpaceMarine(Integer id, boolean deleteCoordinates, boolean deleteChapter) {
        Optional<SpaceMarine> spaceMarine = spaceMarineDAO.findById(id);
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
                long coordinatesUsageCount = spaceMarineDAO.countByCoordinatesId(coordinatesId);
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
                long chapterUsageCount = spaceMarineDAO.countByChapterId(chapterId);
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
                chapterDAO.removeMarineFromChapter(marine.getChapter().getId());
            }
            
            // Удаляем самого марина
            spaceMarineDAO.delete(id);
            
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
        Optional<SpaceMarine> spaceMarine = spaceMarineDAO.findById(id);
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
                long coordinatesUsageCount = spaceMarineDAO.countByCoordinatesId(coordinatesId);
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
                long chapterUsageCount = spaceMarineDAO.countByChapterId(chapterId);
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
                chapterDAO.removeMarineFromChapter(marine.getChapter().getId());
            }
            
            spaceMarineDAO.delete(id);
            
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
        return spaceMarineDAO.findByNameContaining(name);
    }

    public List<SpaceMarine> findSpaceMarinesByHealthLessThan(Integer health) {
        return spaceMarineDAO.findByHealthLessThan(health);
    }

    public long countSpaceMarinesByHealthLessThan(Integer health) {
        return spaceMarineDAO.countByHealthLessThan(health);
    }

    public Double getAverageHeartCount() {
        return spaceMarineDAO.getAverageHeartCount();
    }
    
    public RelatedObjectsResponse getRelatedObjects(Integer id) {
        Optional<SpaceMarine> spaceMarine = spaceMarineDAO.findById(id);
        if (!spaceMarine.isPresent()) {
            throw new IllegalArgumentException("SpaceMarine not found with id: " + id);
        }
        
        SpaceMarine marine = spaceMarine.get();
        RelatedObjectsResponse response = new RelatedObjectsResponse();
        
        if (marine.getCoordinates() != null) {
            response.setHasCoordinates(true);
            response.setCoordinates(spaceMarineMapper.toCoordinatesDTO(marine.getCoordinates()));
        }
        
        if (marine.getChapter() != null) {
            response.setHasChapter(true);
            response.setChapter(spaceMarineMapper.toChapterDTO(marine.getChapter()));
        }
        
        return response;
    }

    @Transactional
    public SpaceMarine removeMarineFromChapter(Integer id) {
        Optional<SpaceMarine> spaceMarineOpt = spaceMarineDAO.findById(id);
        if (!spaceMarineOpt.isPresent()) {
            return null;
        }
        
        SpaceMarine spaceMarine = spaceMarineOpt.get();
        if (spaceMarine.getChapter() == null) {
            throw new IllegalArgumentException("У десантника нет ордена для отчисления");
        }
        
        chapterDAO.removeMarineFromChapter(spaceMarine.getChapter().getId());
        
        spaceMarine.setChapter(null);
        
        return spaceMarineDAO.update(spaceMarine);
    }
}
