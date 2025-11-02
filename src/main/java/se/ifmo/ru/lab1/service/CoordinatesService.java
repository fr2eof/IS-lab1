package se.ifmo.ru.lab1.service;

import se.ifmo.ru.lab1.entity.Coordinates;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;

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
