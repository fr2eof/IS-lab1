package se.ifmo.ru.back.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.dto.CoordinatesDTO;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.entity.Coordinates;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.CoordinatesService;
import se.ifmo.ru.back.ws.SpaceMarineWebSocket;

import java.util.List;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {

    private final CoordinatesService coordinatesService;
    private final SpaceMarineMapper spaceMarineMapper;

    public CoordinatesController(CoordinatesService coordinatesService, SpaceMarineMapper spaceMarineMapper) {
        this.coordinatesService = coordinatesService;
        this.spaceMarineMapper = spaceMarineMapper;
    }

    @GetMapping
    public ResponseEntity<Page<CoordinatesDTO>> getAllCoordinates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        List<Coordinates> coordinates = coordinatesService.getCoordinates(page, size, sortBy, sortOrder);
        long totalCount = coordinatesService.getCoordinatesCount();
        
        List<CoordinatesDTO> coordinatesDTOs = coordinates.stream()
                .map(spaceMarineMapper::toCoordinatesDTO)
                .toList();
        
        Page<CoordinatesDTO> pageResult = new PageImpl<>(
            coordinatesDTOs, 
            PageRequest.of(page, size), 
            totalCount
        );
        
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoordinatesDTO> getCoordinatesById(@PathVariable Long id) {
        return coordinatesService.getCoordinatesById(id)
            .map(spaceMarineMapper::toCoordinatesDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Coordinates", id));
    }

    @PostMapping
    public ResponseEntity<CoordinatesDTO> createCoordinates(@Valid @RequestBody CoordinatesDTO coordinatesDTO) {
        Coordinates coordinates = spaceMarineMapper.toCoordinatesEntity(coordinatesDTO);
        Coordinates createdCoordinates = coordinatesService.createCoordinates(coordinates);
        CoordinatesDTO createdDTO = spaceMarineMapper.toCoordinatesDTO(createdCoordinates);
        
        // Уведомляем всех клиентов о создании координат (в try-catch, чтобы ошибки WebSocket не влияли на HTTP ответ)
        try {
            SpaceMarineWebSocket.broadcast("coordinates_created:" + createdDTO.id());
        } catch (Exception wsException) {
            // Логируем ошибку WebSocket, но не прерываем обработку HTTP запроса
            System.err.println("WebSocket broadcast error: " + wsException.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoordinatesDTO> updateCoordinates(
            @PathVariable Long id, 
            @Valid @RequestBody CoordinatesDTO coordinatesDTO) {
        try {
            Coordinates coordinates = spaceMarineMapper.toCoordinatesEntity(coordinatesDTO);
            Coordinates updatedCoordinates = coordinatesService.updateCoordinates(id, coordinates);
            if (updatedCoordinates != null) {
                CoordinatesDTO updatedDTO = spaceMarineMapper.toCoordinatesDTO(updatedCoordinates);
                
                // Уведомляем всех клиентов об обновлении координат (в try-catch, чтобы ошибки WebSocket не влияли на HTTP ответ)
                try {
                    SpaceMarineWebSocket.broadcast("coordinates_updated:" + id);
                    // Также уведомляем об обновлении SpaceMarine, которые используют эти координаты
                    SpaceMarineWebSocket.broadcast("updated");
                } catch (Exception wsException) {
                    // Логируем ошибку WebSocket, но не прерываем обработку HTTP запроса
                    System.err.println("WebSocket broadcast error: " + wsException.getMessage());
                }
                
                return ResponseEntity.ok(updatedDTO);
            } else {
                throw new EntityNotFoundException("Coordinates", id);
            }
        } catch (Exception e) {
            // GlobalExceptionHandler обработает исключение
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoordinates(@PathVariable Long id) {
        try {
            boolean deleted = coordinatesService.deleteCoordinates(id);
            if (deleted) {
                // Уведомляем всех клиентов об удалении координат (в try-catch, чтобы ошибки WebSocket не влияли на HTTP ответ)
                try {
                    SpaceMarineWebSocket.broadcast("coordinates_deleted:" + id);
                    // Также уведомляем об обновлении SpaceMarine, которые использовали эти координаты
                    SpaceMarineWebSocket.broadcast("updated");
                } catch (Exception wsException) {
                    // Логируем ошибку WebSocket, но не прерываем обработку HTTP запроса
                    System.err.println("WebSocket broadcast error: " + wsException.getMessage());
                }
                return ResponseEntity.noContent().build();
            } else {
                throw new EntityNotFoundException("Coordinates", id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<RelatedObjectsResponse> getRelatedObjects(@PathVariable Long id) {
        try {
            RelatedObjectsResponse related = coordinatesService.getRelatedObjects(id);
            return ResponseEntity.ok(related);
        } catch (Exception e) {
            throw new EntityNotFoundException("Coordinates", id);
        }
    }
}

