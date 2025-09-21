package se.ifmo.ru.lab1.dto;

import java.time.ZonedDateTime;

public class ErrorResponse {
    
    private String error;
    private String type;
    private ZonedDateTime timestamp;
    private String path;
    
    public ErrorResponse(String error, String type) {
        this.error = error;
        this.type = type;
        this.timestamp = ZonedDateTime.now();
    }
    
    public ErrorResponse(String error, String type, String path) {
        this.error = error;
        this.type = type;
        this.path = path;
        this.timestamp = ZonedDateTime.now();
    }
    
    // Getters
    public String getError() {
        return error;
    }
    
    public String getType() {
        return type;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getPath() {
        return path;
    }
}
