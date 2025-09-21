package se.ifmo.ru.lab1.dto;

import jakarta.validation.constraints.NotNull;

public class CoordinatesDTO {
    
    private Long id;
    
    @NotNull(message = "X coordinate cannot be null")
    private Float x;
    
    private double y;

    public CoordinatesDTO() {}

    public CoordinatesDTO(Long id, Float x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
