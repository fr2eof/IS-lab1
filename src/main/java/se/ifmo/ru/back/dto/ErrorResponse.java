package se.ifmo.ru.back.dto;

import java.time.ZonedDateTime;

public record ErrorResponse(
        String error,
        String type,
        ZonedDateTime timestamp,
        String path
) {
    public ErrorResponse(String error, String type) {
        this(error, type, ZonedDateTime.now(), null);
    }

    public ErrorResponse(String error, String type, String path) {
        this(error, type, ZonedDateTime.now(), path);
    }
}
