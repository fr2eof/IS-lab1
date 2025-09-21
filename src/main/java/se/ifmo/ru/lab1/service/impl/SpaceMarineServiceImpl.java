package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.SpaceMarineDAO;
import se.ifmo.ru.lab1.dao.ChapterDAO;
import se.ifmo.ru.lab1.entity.SpaceMarine;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.service.SpaceMarineService;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SpaceMarineServiceImpl implements SpaceMarineService {

    @Inject
    private SpaceMarineDAO spaceMarineDAO;

    @Inject
    private ChapterDAO chapterDAO;

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

    public Optional<SpaceMarine> getSpaceMarineById(Integer id) {
        return spaceMarineDAO.findById(id);
    }

    public List<SpaceMarine> getAllSpaceMarines() {
        return spaceMarineDAO.findAll();
    }

    public List<SpaceMarine> getSpaceMarines(int page, int size) {
        return spaceMarineDAO.findAll(page, size);
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
}
