package se.ifmo.ru.lab1.exception;

public class EntityNotFoundException extends RuntimeException {
    
    private final String entityType;
    private final Object entityId;
    
    public EntityNotFoundException(String entityType, Object entityId) {
        super(String.format("%s with id %s not found", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = null;
        this.entityId = null;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public Object getEntityId() {
        return entityId;
    }
}
