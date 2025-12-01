package se.ifmo.ru.back.service.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

    private final CoordinatesRepository coordinatesRepository;
    private final SpaceMarineRepository spaceMarineRepository;
    private final SpaceMarineMapper spaceMarineMapper;
    private final Validator validator;

    public CoordinatesServiceImpl(
            CoordinatesRepository coordinatesRepository,
            SpaceMarineRepository spaceMarineRepository,
            SpaceMarineMapper spaceMarineMapper,
            Validator validator) {
        this.coordinatesRepository = coordinatesRepository;
        this.spaceMarineRepository = spaceMarineRepository;
        this.spaceMarineMapper = spaceMarineMapper;
        this.validator = validator;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public Coordinates createCoordinates(Coordinates coordinates) {
        // Проверка уникальности с блокировкой для предотвращения race conditions
        Optional<Coordinates> existing = coordinatesRepository.findByXAndYWithLock(
                coordinates.getX(), coordinates.getY());
        if (existing.isPresent()) {
            throw new ValidationException("Координаты с такими значениями x и y уже существуют");
        }
        
        // Валидация сущности (включая кастомные валидаторы)
        Set<ConstraintViolation<Coordinates>> violations = validator.validate(coordinates);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errorMessage);
        }
        
        return coordinatesRepository.save(coordinates);
    }

    public Optional<Coordinates> getCoordinatesById(Long id) {
        return coordinatesRepository.findById(id);
    }

    public List<Coordinates> getAllCoordinates() {
        return coordinatesRepository.findAll();
    }

    public List<Coordinates> getCoordinates(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Coordinates> pageResult = coordinatesRepository.findAll(pageable);
        return pageResult.getContent();
    }

    public List<Coordinates> getCoordinates(int page, int size, String sortBy, String sortOrder) {
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
        return pageResult.getContent();
    }

    public long getCoordinatesCount() {
        return coordinatesRepository.count();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Coordinates updateCoordinates(Long id, Coordinates updatedCoordinates) {
        // Используем блокировку для предотвращения одновременного обновления
        Optional<Coordinates> existingCoordinates = coordinatesRepository.findByIdForUpdate(id);
        if (existingCoordinates.isPresent()) {
            Coordinates coordinates = existingCoordinates.get();
            
            // Проверяем уникальность новых значений (исключая текущий объект)
            if (!coordinates.getX().equals(updatedCoordinates.getX()) || 
                !java.util.Objects.equals(coordinates.getY(), updatedCoordinates.getY())) {
                Optional<Coordinates> duplicate = coordinatesRepository.findByXAndYWithLock(
                        updatedCoordinates.getX(), updatedCoordinates.getY());
                if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                    throw new ValidationException("Координаты с такими значениями x и y уже существуют");
                }
            }
            
            coordinates.setX(updatedCoordinates.getX());
            coordinates.setY(updatedCoordinates.getY());
            
            // Валидация сущности (включая кастомные валидаторы)
            Set<ConstraintViolation<Coordinates>> violations = validator.validate(coordinates);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                throw new ValidationException(errorMessage);
            }
            
            return coordinatesRepository.save(coordinates);
        }
        return null;
    }

    @Transactional
    public boolean deleteCoordinates(Long id) {
        Optional<Coordinates> coordinates = coordinatesRepository.findById(id);
        if (coordinates.isPresent()) {
            // Автоматически удаляем всех маринов, связанных с этими координатами
            long usageCount = spaceMarineRepository.countByCoordinatesId(id);
            if (usageCount > 0) {
                spaceMarineRepository.deleteByCoordinatesId(id);
            }
            
            coordinatesRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public RelatedObjectsResponse getRelatedObjects(Long id) {
        // Находим всех SpaceMarines, которые используют эти координаты
        List<SpaceMarine> relatedMarines = spaceMarineRepository.findByCoordinatesId(id);
        List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        return new RelatedObjectsResponse(relatedMarinesDTO);
    }
}
