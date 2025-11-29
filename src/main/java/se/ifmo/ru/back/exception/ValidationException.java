package se.ifmo.ru.back.exception;

public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, String message) {
        super("Validation failed for field '" + field + "': " + message);
    }
}
