package se.ifmo.ru.back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.service.CacheStatisticsStateService;

import java.util.Map;

/**
 * Контроллер для управления состоянием логирования статистики L2 JPA Cache
 */
@RestController
@RequestMapping("/api/cache-stats")
public class CacheStatisticsController {

    private final CacheStatisticsStateService stateService;

    public CacheStatisticsController(CacheStatisticsStateService stateService) {
        this.stateService = stateService;
    }

    /**
     * Получить текущее состояние логирования статистики
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("enabled", stateService.isStatisticsEnabled()));
    }

    /**
     * Переключить состояние логирования статистики
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Boolean>> toggle() {
        stateService.toggleStatistics();
        return ResponseEntity.ok(Map.of("enabled", stateService.isStatisticsEnabled()));
    }

    /**
     * Установить состояние логирования статистики
     */
    @PostMapping("/set")
    public ResponseEntity<Map<String, Boolean>> setEnabled(@RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled != null) {
            stateService.setStatisticsEnabled(enabled);
        }
        return ResponseEntity.ok(Map.of("enabled", stateService.isStatisticsEnabled()));
    }
}

