package se.ifmo.ru.lab1.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.ifmo.ru.lab1.dto.ChapterDTO;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.exception.EntityNotFoundException;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;
import se.ifmo.ru.lab1.service.ChapterService;

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
    public Response getAllChapters() {
        List<Chapter> chapters = chapterService.getAllChapters();
        List<ChapterDTO> chapterDTOs = chapters.stream()
                .map(spaceMarineMapper::toChapterDTO)
                .collect(java.util.stream.Collectors.toList());
        return Response.ok(chapterDTOs).build();
    }

    @GET
    @Path("/{id}")
    public Response getChapterById(@PathParam("id") Long id) {
        Optional<Chapter> chapter = chapterService.getChapterById(id);
        if (chapter.isPresent()) {
            return Response.ok(chapter.get()).build();
        } else {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    @POST
    public Response createChapter(@Valid Chapter chapter) {
        Chapter createdChapter = chapterService.createChapter(chapter);
        return Response.status(Response.Status.CREATED).entity(createdChapter).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateChapter(@PathParam("id") Long id, @Valid Chapter chapter) {
        Chapter updatedChapter = chapterService.updateChapter(id, chapter);
        if (updatedChapter != null) {
            return Response.ok(updatedChapter).build();
        } else {
            throw new EntityNotFoundException("Chapter", id);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteChapter(@PathParam("id") Long id) {
        boolean deleted = chapterService.deleteChapter(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            throw new EntityNotFoundException("Chapter", id);
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
