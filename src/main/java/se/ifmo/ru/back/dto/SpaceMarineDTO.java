package se.ifmo.ru.back.dto;

import jakarta.validation.constraints.*;

import java.time.ZonedDateTime;

public record SpaceMarineDTO(
        Integer id,

        @NotBlank(message = "Name cannot be null or empty")
        String name,

        CoordinatesDTO coordinates,

        ZonedDateTime creationDate,

        ChapterDTO chapter,

        @NotNull(message = "Health cannot be null")
        @Min(value = 1, message = "Health must be greater than 0")
        Integer health,

        @NotNull(message = "Heart count cannot be null")
        @Min(value = 1, message = "Heart count must be greater than 0")
        @Max(value = 3, message = "Heart count cannot exceed 3")
        Integer heartCount,

        String category,

        String weaponType,

        // Поля для ID связей (используются при создании)
        @NotNull(message = "Coordinates ID cannot be null")
        Long coordinatesId,

        Long chapterId
) {
}
