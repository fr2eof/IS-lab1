package se.ifmo.ru.back.dto;

public record ImportResponseDTO(
        Long importHistoryId,
        String status,
        Integer createdObjectsCount,
        String message,
        String errorMessage
) {
    public ImportResponseDTO(Long importHistoryId, String status, Integer createdObjectsCount, String message) {
        this(importHistoryId, status, createdObjectsCount, message, null);
    }
}
