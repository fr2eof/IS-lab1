package se.ifmo.ru.back.service;

import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.entity.Coordinates;

import java.util.List;
import java.util.Optional;

public interface CoordinatesService {
    
    Coordinates createCoordinates(Coordinates coordinates);
    
    Optional<Coordinates> getCoordinatesById(Long id);
    
    List<Coordinates> getAllCoordinates();
    
    List<Coordinates> getCoordinates(int page, int size);
    
    List<Coordinates> getCoordinates(int page, int size, String sortBy, String sortOrder);
    
    long getCoordinatesCount();
    
    Coordinates updateCoordinates(Long id, Coordinates updatedCoordinates);
    
    boolean deleteCoordinates(Long id);
    
    RelatedObjectsResponse getRelatedObjects(Long id);
}
