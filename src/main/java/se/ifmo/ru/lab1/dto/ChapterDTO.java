package se.ifmo.ru.lab1.dto;

import jakarta.validation.constraints.*;

public class ChapterDTO {
    
    private Long id;
    
    @NotBlank(message = "Chapter name cannot be null or empty")
    private String name;
    
    @NotNull(message = "Marines count cannot be null")
    @Min(value = 1, message = "Marines count must be greater than 0")
    @Max(value = 1000, message = "Marines count cannot exceed 1000")
    private Integer marinesCount;

    public ChapterDTO() {}

    public ChapterDTO(Long id, String name, Integer marinesCount) {
        this.id = id;
        this.name = name;
        this.marinesCount = marinesCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMarinesCount() {
        return marinesCount;
    }

    public void setMarinesCount(Integer marinesCount) {
        this.marinesCount = marinesCount;
    }
}
