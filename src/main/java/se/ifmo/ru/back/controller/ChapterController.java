package se.ifmo.ru.back.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ifmo.ru.back.dto.ChapterDTO;
import se.ifmo.ru.back.dto.ErrorResponse;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.exception.EntityNotFoundException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.ChapterService;
import se.ifmo.ru.back.ws.SpaceMarineWebSocket;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterService chapterService;
    private final SpaceMarineMapper spaceMarineMapper;

    public ChapterController(ChapterService chapterService, SpaceMarineMapper spaceMarineMapper) {
        this.chapterService = chapterService;
        this.spaceMarineMapper = spaceMarineMapper;
    }

    @GetMapping
    public ResponseEntity<Page<ChapterDTO>> getAllChapters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        List<Chapter> chapters = chapterService.getChapters(page, size, sortBy, sortOrder);
        long totalCount = chapterService.getChaptersCount();

        List<ChapterDTO> chapterDTOs = chapters.stream()
                .map(spaceMarineMapper::toChapterDTO)
                .toList();

        Page<ChapterDTO> pageResult = new PageImpl<>(
                chapterDTOs,
                PageRequest.of(page, size),
                totalCount
        );

        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChapterDTO> getChapterById(@PathVariable Long id) {
        return chapterService.getChapterById(id)
                .map(spaceMarineMapper::toChapterDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("Chapter", id));
    }

    @PostMapping
    public ResponseEntity<ChapterDTO> createChapter(@Valid @RequestBody ChapterDTO chapterDTO) {
        Chapter chapter = spaceMarineMapper.toChapterEntity(chapterDTO);
        Chapter createdChapter = chapterService.createChapter(chapter);
        ChapterDTO createdDTO = spaceMarineMapper.toChapterDTO(createdChapter);

        // Уведомляем всех клиентов о создании главы
        SpaceMarineWebSocket.broadcast("chapter_created:" + createdDTO.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChapterDTO> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterDTO chapterDTO) {
        Chapter chapter = spaceMarineMapper.toChapterEntity(chapterDTO);
        Chapter updatedChapter = chapterService.updateChapter(id, chapter);
        if (updatedChapter != null) {
            ChapterDTO updatedDTO = spaceMarineMapper.toChapterDTO(updatedChapter);

            // Уведомляем всех клиентов об обновлении главы
            SpaceMarineWebSocket.broadcast("chapter_updated:" + id);
            // Также уведомляем об обновлении SpaceMarine, которые используют эту главу
            SpaceMarineWebSocket.broadcast("updated");

            return ResponseEntity.ok(updatedDTO);
        } else {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        try {
            boolean deleted = chapterService.deleteChapter(id);
            if (deleted) {
                // Уведомляем всех клиентов об удалении главы
                SpaceMarineWebSocket.broadcast("chapter_deleted:" + id);
                // Также уведомляем об обновлении SpaceMarine, которые использовали эту главу
                SpaceMarineWebSocket.broadcast("updated");
                return ResponseEntity.noContent().build();
            } else {
                throw new EntityNotFoundException("Chapter", id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/remove-marine")
    public ResponseEntity<SuccessResponse> removeMarineFromChapter(@PathVariable Long id) {
        boolean removed = chapterService.removeMarineFromChapter(id);
        if (removed) {
            return ResponseEntity.ok(new SuccessResponse("Marine removed from chapter"));
        } else {
            throw new IllegalArgumentException("Cannot remove marine from chapter");
        }
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<RelatedObjectsResponse> getRelatedObjects(@PathVariable Long id) {
        try {
            RelatedObjectsResponse related = chapterService.getRelatedObjects(id);
            return ResponseEntity.ok(related);
        } catch (Exception e) {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    // Helper record for JSON responses
    public record SuccessResponse(String message) {
    }
}

