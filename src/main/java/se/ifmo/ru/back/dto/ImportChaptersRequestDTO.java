package se.ifmo.ru.back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportChaptersRequestDTO(
        @NotEmpty(message = "Chapters list cannot be empty")
        @Valid
        List<ChapterDTO> chapters
) {
}

