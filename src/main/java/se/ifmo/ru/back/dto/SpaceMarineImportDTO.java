package se.ifmo.ru.back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpaceMarineImportDTO(
        @NotBlank(message = "Name cannot be null or empty")
        String name,
        
        // Либо coordinatesId (для существующих координат), либо coordinates (для создания новых)
        Long coordinatesId,
        
        @Valid
        CoordinatesDTO coordinates,
        
        // Либо chapterId (для существующей главы), либо chapter (для создания новой)
        Long chapterId,
        
        @Valid
        ChapterDTO chapter,
        
        @NotNull(message = "Health cannot be null")
        @Min(value = 1, message = "Health must be greater than 0")
        Integer health,
        
        @NotNull(message = "Heart count cannot be null")
        @Min(value = 1, message = "Heart count must be greater than 0")
        @Max(value = 3, message = "Heart count cannot exceed 3")
        Integer heartCount,
        
        String category,
        
        String weaponType
) {
}
