package se.ifmo.ru.back.service;


import se.ifmo.ru.back.dto.DeleteResponse;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.dto.SpaceMarineDTO;
import se.ifmo.ru.back.entity.SpaceMarine;

import java.util.List;
import java.util.Optional;

public interface SpaceMarineService {
    
    SpaceMarine createSpaceMarine(SpaceMarine spaceMarine);
    
    SpaceMarine createSpaceMarineFromDTO(SpaceMarineDTO dto);
    
    Optional<SpaceMarine> getSpaceMarineById(Integer id);
    
    List<SpaceMarine> getAllSpaceMarines();
    
    List<SpaceMarine> getSpaceMarines(int page, int size);
    
    List<SpaceMarine> getSpaceMarines(int page, int size, String sortBy, String sortOrder);
    
    List<SpaceMarine> getSpaceMarinesWithFilters(String nameFilter, String sortBy, String sortOrder, int page, int size);
    
    long getSpaceMarinesCount();
    
    long getSpaceMarinesCountWithFilters(String nameFilter);
    
    SpaceMarine updateSpaceMarine(Integer id, SpaceMarine updatedSpaceMarine);
    
    SpaceMarine updateSpaceMarineFromDTO(Integer id, SpaceMarineDTO dto);
    
    boolean deleteSpaceMarine(Integer id);
    
    boolean deleteSpaceMarine(Integer id, boolean deleteCoordinates, boolean deleteChapter);
    
    DeleteResponse deleteSpaceMarineWithDetails(Integer id, boolean deleteCoordinates, boolean deleteChapter);
    
    List<SpaceMarine> findSpaceMarinesByNameContaining(String name);
    
    List<SpaceMarine> findSpaceMarinesByHealthLessThan(Integer health);
    
    long countSpaceMarinesByHealthLessThan(Integer health);
    
    Double getAverageHeartCount();
    
    RelatedObjectsResponse getRelatedObjects(Integer id);
    
    SpaceMarine removeMarineFromChapter(Integer id);
}
