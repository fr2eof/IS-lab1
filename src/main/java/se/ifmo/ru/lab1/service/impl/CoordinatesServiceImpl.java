package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.CoordinatesDAO;
import se.ifmo.ru.lab1.dao.SpaceMarineDAO;
import se.ifmo.ru.lab1.entity.Coordinates;
import se.ifmo.ru.lab1.entity.SpaceMarine;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;
import se.ifmo.ru.lab1.dto.SpaceMarineDTO;
import se.ifmo.ru.lab1.service.CoordinatesService;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CoordinatesServiceImpl implements CoordinatesService {

    @Inject
    private CoordinatesDAO coordinatesDAO;
    
    @Inject
    private SpaceMarineDAO spaceMarineDAO;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @Transactional
    public Coordinates createCoordinates(Coordinates coordinates) {
        return coordinatesDAO.save(coordinates);
    }

    public Optional<Coordinates> getCoordinatesById(Long id) {
        return coordinatesDAO.findById(id);
    }

    public List<Coordinates> getAllCoordinates() {
        return coordinatesDAO.findAll();
    }

    public List<Coordinates> getCoordinates(int page, int size) {
        return coordinatesDAO.findAll(page, size);
    }

    public List<Coordinates> getCoordinates(int page, int size, String sortBy, String sortOrder) {
        return coordinatesDAO.findAll(page, size, sortBy, sortOrder);
    }

    public long getCoordinatesCount() {
        return coordinatesDAO.count();
    }

    @Transactional
    public Coordinates updateCoordinates(Long id, Coordinates updatedCoordinates) {
        Optional<Coordinates> existingCoordinates = coordinatesDAO.findById(id);
        if (existingCoordinates.isPresent()) {
            Coordinates coordinates = existingCoordinates.get();
            coordinates.setX(updatedCoordinates.getX());
            coordinates.setY(updatedCoordinates.getY());
            return coordinatesDAO.update(coordinates);
        }
        return null;
    }

    @Transactional
    public boolean deleteCoordinates(Long id) {
        Optional<Coordinates> coordinates = coordinatesDAO.findById(id);
        if (coordinates.isPresent()) {
            // Автоматически удаляем всех маринов, связанных с этими координатами
            long usageCount = spaceMarineDAO.countByCoordinatesId(id);
            if (usageCount > 0) {
                spaceMarineDAO.deleteByCoordinatesId(id);
            }
            
            coordinatesDAO.delete(id);
            return true;
        }
        return false;
    }
    
    public RelatedObjectsResponse getRelatedObjects(Long id) {
        // Находим всех SpaceMarines, которые используют эти координаты
        List<SpaceMarine> relatedMarines = spaceMarineDAO.findByCoordinatesId(id);
        List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        return new RelatedObjectsResponse(relatedMarinesDTO);
    }
}
