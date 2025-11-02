package se.ifmo.ru.lab1.dto;

import jakarta.validation.constraints.*;
import java.time.ZonedDateTime;

public class SpaceMarineDTO {
    
    private Integer id;
    
    @NotBlank(message = "Name cannot be null or empty")
    private String name;
    
    private CoordinatesDTO coordinates;
    
    private ZonedDateTime creationDate;
    
    private ChapterDTO chapter;
    
    @NotNull(message = "Health cannot be null")
    @Min(value = 1, message = "Health must be greater than 0")
    private Integer health;
    
    @NotNull(message = "Heart count cannot be null")
    @Min(value = 1, message = "Heart count must be greater than 0")
    @Max(value = 3, message = "Heart count cannot exceed 3")
    private Integer heartCount;
    
    private String category;
    
    private String weaponType;
    
    // Поля для ID связей (используются при создании)
    @NotNull(message = "Coordinates ID cannot be null")
    private Long coordinatesId;
    
    private Long chapterId;

    public SpaceMarineDTO() {}

    public SpaceMarineDTO(Integer id, String name, CoordinatesDTO coordinates, 
                         ZonedDateTime creationDate, ChapterDTO chapter, 
                         Integer health, Integer heartCount, String category, String weaponType) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.chapter = chapter;
        this.health = health;
        this.heartCount = heartCount;
        this.category = category;
        this.weaponType = weaponType;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinatesDTO getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(CoordinatesDTO coordinates) {
        this.coordinates = coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public ChapterDTO getChapter() {
        return chapter;
    }

    public void setChapter(ChapterDTO chapter) {
        this.chapter = chapter;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getHeartCount() {
        return heartCount;
    }

    public void setHeartCount(Integer heartCount) {
        this.heartCount = heartCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(String weaponType) {
        this.weaponType = weaponType;
    }
    
    public Long getCoordinatesId() {
        return coordinatesId;
    }
    
    public void setCoordinatesId(Long coordinatesId) {
        this.coordinatesId = coordinatesId;
    }
    
    public Long getChapterId() {
        return chapterId;
    }
    
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }
}
