package se.ifmo.ru.lab1.dto;

public class DeleteResponse {
    
    private boolean coordinatesDeleted;
    private boolean chapterDeleted;
    private String message;
    
    public DeleteResponse(boolean coordinatesDeleted, boolean chapterDeleted, String message) {
        this.coordinatesDeleted = coordinatesDeleted;
        this.chapterDeleted = chapterDeleted;
        this.message = message;
    }
    
    public DeleteResponse(String message) {
        this.coordinatesDeleted = false;
        this.chapterDeleted = false;
        this.message = message;
    }
    
    public boolean isCoordinatesDeleted() {
        return coordinatesDeleted;
    }
    
    public void setCoordinatesDeleted(boolean coordinatesDeleted) {
        this.coordinatesDeleted = coordinatesDeleted;
    }
    
    public boolean isChapterDeleted() {
        return chapterDeleted;
    }
    
    public void setChapterDeleted(boolean chapterDeleted) {
        this.chapterDeleted = chapterDeleted;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
