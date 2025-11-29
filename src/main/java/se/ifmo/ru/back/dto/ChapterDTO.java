package se.ifmo.ru.back.dto;

import jakarta.validation.constraints.*;

public record ChapterDTO(
        Long id,

        @NotBlank(message = "Chapter name cannot be null or empty")
        String name,

        @NotNull(message = "Marines count cannot be null")
        @Min(value = 1, message = "Marines count must be greater than 0")
        @Max(value = 1000, message = "Marines count cannot exceed 1000")
        Integer marinesCount
) {
}
