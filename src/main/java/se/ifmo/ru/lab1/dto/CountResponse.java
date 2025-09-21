package se.ifmo.ru.lab1.dto;

public class CountResponse {
    
    private long count;
    
    public CountResponse(long count) {
        this.count = count;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
}
