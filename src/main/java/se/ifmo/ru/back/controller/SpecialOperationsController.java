package se.ifmo.ru.back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.service.SpecialOperationsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/special-operations")
public class SpecialOperationsController {

    private final SpecialOperationsService specialOperationsService;

    public SpecialOperationsController(SpecialOperationsService specialOperationsService) {
        this.specialOperationsService = specialOperationsService;
    }

    @GetMapping("/average-heart-count")
    public ResponseEntity<Map<String, Double>> getAverageHeartCount() {
        Double average = specialOperationsService.getAverageHeartCount();
        return ResponseEntity.ok(Map.of("average", average));
    }

    @GetMapping("/count-by-health")
    public ResponseEntity<Map<String, Integer>> countMarinesByHealth(@RequestParam Integer health) {
        if (health == null) {
            throw new IllegalArgumentException("Health parameter is required");
        }
        
        Integer count = specialOperationsService.countMarinesByHealth(health);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/search-by-name")
    public ResponseEntity<Map<String, List<Object[]>>> findMarinesByNameContaining(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name parameter is required");
        }
        
        List<Object[]> results = specialOperationsService.findMarinesByNameContaining(name);
        return ResponseEntity.ok(Map.of("marines", results));
    }

    @PostMapping("/create-chapter")
    public ResponseEntity<Chapter> createNewChapter(
            @RequestParam String name, 
            @RequestParam Integer marinesCount) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Chapter name is required");
        }
        
        if (marinesCount == null || marinesCount < 1 || marinesCount > 1000) {
            throw new IllegalArgumentException("Marines count must be between 1 and 1000");
        }
        
        Chapter chapter = specialOperationsService.createNewChapter(name, marinesCount);
        return ResponseEntity.ok(chapter);
    }

    @PostMapping("/remove-marine-from-chapter")
    public ResponseEntity<Map<String, Object>> removeMarineFromChapter(@RequestParam Long chapterId) {
        if (chapterId == null) {
            throw new IllegalArgumentException("Chapter ID is required");
        }
        
        boolean removed = specialOperationsService.removeMarineFromChapter(chapterId);
        if (removed) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Marine removed from chapter"));
        } else {
            throw new IllegalArgumentException("Could not remove marine from chapter");
        }
    }
}

