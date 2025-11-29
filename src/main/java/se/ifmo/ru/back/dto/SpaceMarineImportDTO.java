package se.ifmo.ru.back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpaceMarineImportDTO(
        @NotBlank(message = "Name cannot be null or empty")
        String name,
        
        @Valid
        @NotNull(message = "Coordinates cannot be null")
        CoordinatesDTO coordinates,
        
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
