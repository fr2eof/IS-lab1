package se.ifmo.ru.back.dto;

public record DeleteResponse(
    boolean coordinatesDeleted,
    boolean chapterDeleted,
    String message
) {
    public DeleteResponse(String message) {
        this(false, false, message);
    }
}
