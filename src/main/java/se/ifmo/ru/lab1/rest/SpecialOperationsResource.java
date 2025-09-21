package se.ifmo.ru.lab1.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.service.SpecialOperationsService;

import java.util.List;
import java.util.Map;

@Path("/special-operations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpecialOperationsResource {

    @Inject
    private SpecialOperationsService specialOperationsService;

    @GET
    @Path("/average-heart-count")
    public Response getAverageHeartCount() {
        Double average = specialOperationsService.getAverageHeartCount();
        return Response.ok(Map.of("average", average)).build();
    }

    @GET
    @Path("/count-by-health")
    public Response countMarinesByHealth(@QueryParam("health") Integer health) {
        if (health == null) {
            throw new IllegalArgumentException("Health parameter is required");
        }
        
        Integer count = specialOperationsService.countMarinesByHealth(health);
        return Response.ok(Map.of("count", count)).build();
    }

    @GET
    @Path("/search-by-name")
    public Response findMarinesByNameContaining(@QueryParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name parameter is required");
        }
        
        List<Object[]> results = specialOperationsService.findMarinesByNameContaining(name);
        return Response.ok(Map.of("marines", results)).build();
    }

    @POST
    @Path("/create-chapter")
    public Response createNewChapter(@QueryParam("name") String name, @QueryParam("marinesCount") Integer marinesCount) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Chapter name is required");
        }
        
        if (marinesCount == null || marinesCount < 1 || marinesCount > 1000) {
            throw new IllegalArgumentException("Marines count must be between 1 and 1000");
        }
        
        Chapter chapter = specialOperationsService.createNewChapter(name, marinesCount);
        return Response.ok(chapter).build();
    }

    @POST
    @Path("/remove-marine-from-chapter")
    public Response removeMarineFromChapter(@QueryParam("chapterId") Long chapterId) {
        if (chapterId == null) {
            throw new IllegalArgumentException("Chapter ID is required");
        }
        
        boolean removed = specialOperationsService.removeMarineFromChapter(chapterId);
        if (removed) {
            return Response.ok(Map.of("success", true, "message", "Marine removed from chapter")).build();
        } else {
            throw new IllegalArgumentException("Could not remove marine from chapter");
        }
    }
}
