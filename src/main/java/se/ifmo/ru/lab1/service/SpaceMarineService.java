package se.ifmo.ru.lab1.service;

import se.ifmo.ru.lab1.entity.SpaceMarine;

import java.util.List;
import java.util.Optional;

public interface SpaceMarineService {
    
    SpaceMarine createSpaceMarine(SpaceMarine spaceMarine);
    
    Optional<SpaceMarine> getSpaceMarineById(Integer id);
    
    List<SpaceMarine> getAllSpaceMarines();
    
    List<SpaceMarine> getSpaceMarines(int page, int size);
    
    List<SpaceMarine> getSpaceMarinesWithFilters(String nameFilter, String sortBy, String sortOrder, int page, int size);
    
    long getSpaceMarinesCount();
    
    long getSpaceMarinesCountWithFilters(String nameFilter);
    
    SpaceMarine updateSpaceMarine(Integer id, SpaceMarine updatedSpaceMarine);
    
    boolean deleteSpaceMarine(Integer id);
    
    List<SpaceMarine> findSpaceMarinesByNameContaining(String name);
    
    List<SpaceMarine> findSpaceMarinesByHealthLessThan(Integer health);
    
    long countSpaceMarinesByHealthLessThan(Integer health);
    
    Double getAverageHeartCount();
}
