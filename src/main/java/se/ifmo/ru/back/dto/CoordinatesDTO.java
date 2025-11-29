package se.ifmo.ru.back.dto;

import jakarta.validation.constraints.NotNull;

public record CoordinatesDTO(
        Long id,

        @NotNull(message = "X coordinate cannot be null")
        Float x,

        Double y
) {
}
