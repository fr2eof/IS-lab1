package se.ifmo.ru.lab1.dto;

public class AverageResponse {
    
    private Double average;
    
    public AverageResponse(Double average) {
        this.average = average;
    }
    
    public Double getAverage() {
        return average;
    }
    
    public void setAverage(Double average) {
        this.average = average;
    }
}
