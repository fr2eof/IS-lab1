package se.ifmo.ru.back.service;

import org.springframework.data.domain.Page;
import se.ifmo.ru.back.dto.ImportHistoryDTO;
import se.ifmo.ru.back.dto.ImportRequestDTO;
import se.ifmo.ru.back.dto.ImportResponseDTO;

import java.util.List;

public interface ImportService {
    
    ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username);
    
    ImportResponseDTO importSpaceMarines(ImportRequestDTO request, String username, Long existingHistoryId);
    
    ImportResponseDTO importFromFile(String fileContent, String username);
    
    ImportResponseDTO recordImportError(String username, String errorMessage);
    
    List<ImportHistoryDTO> getImportHistory(String username, boolean isAdmin);
    
    Page<ImportHistoryDTO> getImportHistory(String username, boolean isAdmin, int page, int size);
    
    ImportHistoryDTO getImportHistoryById(Long id, String username, boolean isAdmin);
}
