package se.ifmo.ru.back.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.dto.*;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.SpaceMarineService;
import se.ifmo.ru.back.ws.SpaceMarineWebSocket;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spacemarines")
public class SpaceMarineController {

    private final SpaceMarineService spaceMarineService;
    private final SpaceMarineMapper spaceMarineMapper;

    public SpaceMarineController(SpaceMarineService spaceMarineService, SpaceMarineMapper spaceMarineMapper) {
        this.spaceMarineService = spaceMarineService;
        this.spaceMarineMapper = spaceMarineMapper;
    }

    @GetMapping
    public ResponseEntity<Page<SpaceMarineDTO>> getAllSpaceMarines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        
        List<SpaceMarine> spaceMarines;
        long totalCount;
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            spaceMarines = spaceMarineService.getSpaceMarinesWithFilters(nameFilter, sortBy, sortOrder, page, size);
            totalCount = spaceMarineService.getSpaceMarinesCountWithFilters(nameFilter);
        } else {
            spaceMarines = spaceMarineService.getSpaceMarines(page, size, sortBy, sortOrder);
            totalCount = spaceMarineService.getSpaceMarinesCount();
        }
        
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        Page<SpaceMarineDTO> pageResult = new PageImpl<>(
            spaceMarineDTOs, 
            PageRequest.of(page, size), 
            totalCount
        );
        
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceMarineDTO> getSpaceMarineById(@PathVariable Integer id) {
        return spaceMarineService.getSpaceMarineById(id)
            .map(spaceMarineMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("SpaceMarine", id));
    }

    @PostMapping
    public ResponseEntity<SpaceMarineDTO> createSpaceMarine(@RequestBody SpaceMarineDTO spaceMarineDTO) {
        SpaceMarine createdSpaceMarine = spaceMarineService.createSpaceMarineFromDTO(spaceMarineDTO);
        SpaceMarineDTO createdDTO = spaceMarineMapper.toDTO(createdSpaceMarine);
        
        // Уведомляем всех клиентов о создании
        SpaceMarineWebSocket.broadcast("created:" + createdDTO.id());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpaceMarineDTO> updateSpaceMarine(
            @PathVariable Integer id, 
            @RequestBody SpaceMarineDTO spaceMarineDTO) {
        SpaceMarine updatedSpaceMarine = spaceMarineService.updateSpaceMarineFromDTO(id, spaceMarineDTO);
        if (updatedSpaceMarine != null) {
            SpaceMarineDTO updatedDTO = spaceMarineMapper.toDTO(updatedSpaceMarine);
            
            // Уведомляем всех клиентов об обновлении
            SpaceMarineWebSocket.broadcast("updated:" + id);
            
            return ResponseEntity.ok(updatedDTO);
        } else {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> deleteSpaceMarine(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean deleteCoordinates,
            @RequestParam(defaultValue = "false") boolean deleteChapter) {
        DeleteResponse deleteResult = spaceMarineService.deleteSpaceMarineWithDetails(id, deleteCoordinates, deleteChapter);
        if (deleteResult.message().contains("Десантник удален")) {
            // Уведомляем всех клиентов об удалении
            SpaceMarineWebSocket.broadcast("deleted:" + id);
            return ResponseEntity.ok(deleteResult);
        } else if (deleteResult.message().contains("не найден")) {
            throw new EntityNotFoundException("SpaceMarine", id);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(deleteResult);
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<SpaceMarineDTO>> findByNameContaining(@RequestParam String name) {
        List<SpaceMarine> spaceMarines = spaceMarineService.findSpaceMarinesByNameContaining(name);
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(spaceMarineDTOs);
    }

    @GetMapping("/search/health")
    public ResponseEntity<List<SpaceMarineDTO>> findByHealthLessThan(@RequestParam Integer health) {
        List<SpaceMarine> spaceMarines = spaceMarineService.findSpaceMarinesByHealthLessThan(health);
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(spaceMarineDTOs);
    }

    @GetMapping("/count/health")
    public ResponseEntity<CountResponse> countByHealthLessThan(@RequestParam Integer health) {
        long count = spaceMarineService.countSpaceMarinesByHealthLessThan(health);
        return ResponseEntity.ok(new CountResponse(count));
    }

    @GetMapping("/stats/average-heart-count")
    public ResponseEntity<AverageResponse> getAverageHeartCount() {
        Double average = spaceMarineService.getAverageHeartCount();
        return ResponseEntity.ok(new AverageResponse(average));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<RelatedObjectsResponse> getRelatedObjects(@PathVariable Integer id) {
        try {
            RelatedObjectsResponse related = spaceMarineService.getRelatedObjects(id);
            return ResponseEntity.ok(related);
        } catch (Exception e) {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
    }

    @PutMapping("/{id}/remove-from-chapter")
    public ResponseEntity<Void> removeFromChapter(@PathVariable Integer id) {
        try {
            SpaceMarine spaceMarine = spaceMarineService.removeMarineFromChapter(id);
            if (spaceMarine == null) {
                throw new EntityNotFoundException("SpaceMarine", id);
            }
            
            // Уведомляем клиентов об обновлении
            SpaceMarineWebSocket.broadcast("updated");
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

