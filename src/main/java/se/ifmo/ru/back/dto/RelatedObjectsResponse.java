package se.ifmo.ru.back.dto;

import java.util.List;

public record RelatedObjectsResponse(
        boolean hasCoordinates,
        CoordinatesDTO coordinates,
        boolean hasChapter,
        ChapterDTO chapter,
        List<SpaceMarineDTO> relatedSpaceMarines
) {
    public RelatedObjectsResponse(List<SpaceMarineDTO> relatedSpaceMarines) {
        this(false, null, false, null, relatedSpaceMarines);
    }

    public RelatedObjectsResponse(boolean hasCoordinates, CoordinatesDTO coordinates,
                                  boolean hasChapter, ChapterDTO chapter) {
        this(hasCoordinates, coordinates, hasChapter, chapter, null);
    }
}
