package se.ifmo.ru.back.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.dto.ImportHistoryDTO;
import se.ifmo.ru.back.dto.ImportRequestDTO;
import se.ifmo.ru.back.dto.ImportResponseDTO;
import se.ifmo.ru.back.exception.AccessDeniedException;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.service.ImportService;
import se.ifmo.ru.back.ws.SpaceMarineWebSocket;

import java.util.List;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    // Список администраторов (можно вынести в конфигурацию)
    private static final java.util.Set<String> ADMIN_USERS = java.util.Set.of("admin", "administrator");

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/file", consumes = "text/plain")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ImportResponseDTO> importFromFile(
            @RequestBody String fileContent,
            @RequestParam(defaultValue = "user") String username) {
        try {
            ImportResponseDTO response = importService.importFromFile(fileContent, username);
            
            if ("SUCCESS".equals(response.status())) {
                // Уведомляем всех клиентов об импорте
                SpaceMarineWebSocket.broadcast("imported:" + response.createdObjectsCount());
                
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(response);
            }
        } catch (Exception e) {
            ImportResponseDTO errorResponse = new ImportResponseDTO(
                    null, 
                    "FAILED", 
                    0, 
                    "Ошибка импорта",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
        }
    }

    @PostMapping("/spacemarines")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ImportResponseDTO> importSpaceMarines(
            @Valid @RequestBody ImportRequestDTO request,
            @RequestParam(defaultValue = "user") String username) {
        try {
            ImportResponseDTO response = importService.importSpaceMarines(request, username);
            
            if ("SUCCESS".equals(response.status())) {
                // Уведомляем всех клиентов об импорте
                SpaceMarineWebSocket.broadcast("imported:" + response.createdObjectsCount());
                
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(response);
            }
        } catch (Exception e) {
            ImportResponseDTO errorResponse = new ImportResponseDTO(
                    null,
                    "FAILED",
                    0,
                    "Ошибка импорта",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Page<ImportHistoryDTO>> getImportHistory(
            @RequestParam(defaultValue = "user") String username,
            @RequestParam(defaultValue = "false") boolean isAdmin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Проверяем, является ли пользователь администратором
        boolean actualIsAdmin = isAdmin || ADMIN_USERS.contains(username.toLowerCase());
        
        Page<ImportHistoryDTO> history = importService.getImportHistory(username, actualIsAdmin, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ImportHistoryDTO> getImportHistoryById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "user") String username,
            @RequestParam(defaultValue = "false") boolean isAdmin) {
        
        // Проверяем, является ли пользователь администратором
        boolean actualIsAdmin = isAdmin || ADMIN_USERS.contains(username.toLowerCase());
        
        try {
            ImportHistoryDTO history = importService.getImportHistoryById(id, username, actualIsAdmin);
            return ResponseEntity.ok(history);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            String errorMessage = "Ошибка при загрузке истории импорта";
            if (e.getMessage() != null) {
                String msg = e.getMessage().toLowerCase();
                if (msg.contains("connection") || msg.contains("could not") || msg.contains("database")) {
                    errorMessage = "Ошибка подключения к базе данных";
                } else if (msg.length() < 100) {
                    errorMessage = e.getMessage();
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

