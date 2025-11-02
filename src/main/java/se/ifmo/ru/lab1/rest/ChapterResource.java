package se.ifmo.ru.lab1.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.ifmo.ru.lab1.dto.ChapterDTO;
import se.ifmo.ru.lab1.dto.PageResponse;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.exception.EntityNotFoundException;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;
import se.ifmo.ru.lab1.service.ChapterService;
import se.ifmo.ru.lab1.ws.SpaceMarineWebSocket;

import java.util.List;
import java.util.Optional;

@Path("/chapters")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChapterResource {

    @Inject
    private ChapterService chapterService;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @GET
    public Response getAllChapters(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {
        List<Chapter> chapters;
        long totalCount;
        
        chapters = chapterService.getChapters(page, size, sortBy, sortOrder);
        totalCount = chapterService.getChaptersCount();
        
        List<ChapterDTO> chapterDTOs = chapters.stream()
                .map(spaceMarineMapper::toChapterDTO)
                .collect(java.util.stream.Collectors.toList());
        
        return Response.ok()
                .entity(new PageResponse<>(chapterDTOs, totalCount, page, size))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getChapterById(@PathParam("id") Long id) {
        Optional<Chapter> chapter = chapterService.getChapterById(id);
        if (chapter.isPresent()) {
            ChapterDTO dto = spaceMarineMapper.toChapterDTO(chapter.get());
            return Response.ok(dto).build();
        } else {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    @POST
    public Response createChapter(@Valid ChapterDTO chapterDTO) {
        Chapter chapter = spaceMarineMapper.toChapterEntity(chapterDTO);
        Chapter createdChapter = chapterService.createChapter(chapter);
        ChapterDTO createdDTO = spaceMarineMapper.toChapterDTO(createdChapter);
        
        // Уведомляем всех клиентов о создании главы
        SpaceMarineWebSocket.broadcast("chapter_created:" + createdDTO.getId());
        
        return Response.status(Response.Status.CREATED).entity(createdDTO).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateChapter(@PathParam("id") Long id, @Valid ChapterDTO chapterDTO) {
        Chapter chapter = spaceMarineMapper.toChapterEntity(chapterDTO);
        Chapter updatedChapter = chapterService.updateChapter(id, chapter);
        if (updatedChapter != null) {
            ChapterDTO updatedDTO = spaceMarineMapper.toChapterDTO(updatedChapter);
            
            // Уведомляем всех клиентов об обновлении главы
            SpaceMarineWebSocket.broadcast("chapter_updated:" + id);
            // Также уведомляем об обновлении SpaceMarine, которые используют эту главу
            SpaceMarineWebSocket.broadcast("updated");
            
            return Response.ok(updatedDTO).build();
        } else {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteChapter(@PathParam("id") Long id) {
        try {
            boolean deleted = chapterService.deleteChapter(id);
            if (deleted) {
                // Уведомляем всех клиентов об удалении главы
                SpaceMarineWebSocket.broadcast("chapter_deleted:" + id);
                // Также уведомляем об обновлении SpaceMarine, которые использовали эту главу
                SpaceMarineWebSocket.broadcast("updated");
                return Response.noContent().build();
            } else {
                throw new EntityNotFoundException("Chapter", id);
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/remove-marine")
    public Response removeMarineFromChapter(@PathParam("id") Long id) {
        boolean removed = chapterService.removeMarineFromChapter(id);
        if (removed) {
            return Response.ok(new SuccessResponse("Marine removed from chapter")).build();
        } else {
            throw new IllegalArgumentException("Cannot remove marine from chapter");
        }
    }

    @GET
    @Path("/{id}/related")
    public Response getRelatedObjects(@PathParam("id") Long id) {
        try {
            RelatedObjectsResponse related = chapterService.getRelatedObjects(id);
            return Response.ok(related).build();
        } catch (Exception e) {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    // Helper classes for JSON responses
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {
        public String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}
