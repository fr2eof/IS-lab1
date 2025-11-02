package se.ifmo.ru.lab1.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.ifmo.ru.lab1.dto.CoordinatesDTO;
import se.ifmo.ru.lab1.dto.PageResponse;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;
import se.ifmo.ru.lab1.entity.Coordinates;
import se.ifmo.ru.lab1.exception.EntityNotFoundException;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;
import se.ifmo.ru.lab1.service.CoordinatesService;
import se.ifmo.ru.lab1.ws.SpaceMarineWebSocket;

import java.util.List;
import java.util.Optional;

@Path("/coordinates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoordinatesResource {

    @Inject
    private CoordinatesService coordinatesService;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @GET
    public Response getAllCoordinates(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {
        List<Coordinates> coordinates;
        long totalCount;
        
        coordinates = coordinatesService.getCoordinates(page, size, sortBy, sortOrder);
        totalCount = coordinatesService.getCoordinatesCount();
        
        List<CoordinatesDTO> coordinatesDTOs = coordinates.stream()
                .map(spaceMarineMapper::toCoordinatesDTO)
                .collect(java.util.stream.Collectors.toList());
        
        return Response.ok()
                .entity(new PageResponse<>(coordinatesDTOs, totalCount, page, size))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getCoordinatesById(@PathParam("id") Long id) {
        Optional<Coordinates> coordinates = coordinatesService.getCoordinatesById(id);
        if (coordinates.isPresent()) {
            CoordinatesDTO dto = spaceMarineMapper.toCoordinatesDTO(coordinates.get());
            return Response.ok(dto).build();
        } else {
            throw new EntityNotFoundException("Coordinates", id);
        }
    }

    @POST
    public Response createCoordinates(@Valid CoordinatesDTO coordinatesDTO) {
        Coordinates coordinates = spaceMarineMapper.toCoordinatesEntity(coordinatesDTO);
        Coordinates createdCoordinates = coordinatesService.createCoordinates(coordinates);
        CoordinatesDTO createdDTO = spaceMarineMapper.toCoordinatesDTO(createdCoordinates);
        
        // Уведомляем всех клиентов о создании координат
        SpaceMarineWebSocket.broadcast("coordinates_created:" + createdDTO.getId());
        
        return Response.status(Response.Status.CREATED).entity(createdDTO).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCoordinates(@PathParam("id") Long id, @Valid CoordinatesDTO coordinatesDTO) {
        Coordinates coordinates = spaceMarineMapper.toCoordinatesEntity(coordinatesDTO);
        Coordinates updatedCoordinates = coordinatesService.updateCoordinates(id, coordinates);
        if (updatedCoordinates != null) {
            CoordinatesDTO updatedDTO = spaceMarineMapper.toCoordinatesDTO(updatedCoordinates);
            
            // Уведомляем всех клиентов об обновлении координат
            SpaceMarineWebSocket.broadcast("coordinates_updated:" + id);
            // Также уведомляем об обновлении SpaceMarine, которые используют эти координаты
            SpaceMarineWebSocket.broadcast("updated");
            
            return Response.ok(updatedDTO).build();
        } else {
            throw new EntityNotFoundException("Coordinates", id);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCoordinates(@PathParam("id") Long id) {
        try {
            boolean deleted = coordinatesService.deleteCoordinates(id);
            if (deleted) {
                // Уведомляем всех клиентов об удалении координат
                SpaceMarineWebSocket.broadcast("coordinates_deleted:" + id);
                // Также уведомляем об обновлении SpaceMarine, которые использовали эти координаты
                SpaceMarineWebSocket.broadcast("updated");
                return Response.noContent().build();
            } else {
                throw new EntityNotFoundException("Coordinates", id);
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    // Helper class for error responses
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    @GET
    @Path("/{id}/related")
    public Response getRelatedObjects(@PathParam("id") Long id) {
        try {
            RelatedObjectsResponse related = coordinatesService.getRelatedObjects(id);
            return Response.ok(related).build();
        } catch (Exception e) {
            throw new EntityNotFoundException("Coordinates", id);
        }
    }
}
