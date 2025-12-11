package se.ifmo.ru.back.dto;

import java.time.ZonedDateTime;

public record ImportHistoryDTO(
        Long id,
        String username,
        String status,
        Integer createdObjectsCount,
        String errorMessage,
        String filePath,
        ZonedDateTime createdAt
) {
}
