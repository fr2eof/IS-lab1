package se.ifmo.ru.back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportCoordinatesRequestDTO(
        @NotEmpty(message = "Coordinates list cannot be empty")
        @Valid
        List<CoordinatesDTO> coordinates
) {
}

