package se.ifmo.ru.back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportRequestDTO(
        @NotEmpty(message = "Space marines list cannot be empty")
        @Valid
        List<SpaceMarineImportDTO> spaceMarines
) {
}
