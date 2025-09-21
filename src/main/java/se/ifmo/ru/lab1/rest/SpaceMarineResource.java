package se.ifmo.ru.lab1.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.ifmo.ru.lab1.dto.SpaceMarineDTO;
import se.ifmo.ru.lab1.dto.PageResponse;
import se.ifmo.ru.lab1.dto.CountResponse;
import se.ifmo.ru.lab1.dto.AverageResponse;
import se.ifmo.ru.lab1.entity.SpaceMarine;
import se.ifmo.ru.lab1.exception.EntityNotFoundException;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;
import se.ifmo.ru.lab1.service.SpaceMarineService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/spacemarines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpaceMarineResource {

    @Inject
    private SpaceMarineService spaceMarineService;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @GET
    public Response getAllSpaceMarines(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {
        
        List<SpaceMarine> spaceMarines;
        long totalCount;
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            spaceMarines = spaceMarineService.getSpaceMarinesWithFilters(nameFilter, sortBy, sortOrder, page, size);
            totalCount = spaceMarineService.getSpaceMarinesCountWithFilters(nameFilter);
        } else {
            spaceMarines = spaceMarineService.getSpaceMarines(page, size);
            totalCount = spaceMarineService.getSpaceMarinesCount();
        }
        
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        return Response.ok()
                .entity(new PageResponse<>(spaceMarineDTOs, totalCount, page, size))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getSpaceMarineById(@PathParam("id") Integer id) {
        Optional<SpaceMarine> spaceMarine = spaceMarineService.getSpaceMarineById(id);
        if (spaceMarine.isPresent()) {
            SpaceMarineDTO dto = spaceMarineMapper.toDTO(spaceMarine.get());
            return Response.ok(dto).build();
        } else {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
    }

    @POST
    public Response createSpaceMarine(@Valid SpaceMarineDTO spaceMarineDTO) {
        SpaceMarine spaceMarine = spaceMarineMapper.toEntity(spaceMarineDTO);
        SpaceMarine createdSpaceMarine = spaceMarineService.createSpaceMarine(spaceMarine);
        SpaceMarineDTO createdDTO = spaceMarineMapper.toDTO(createdSpaceMarine);
        return Response.status(Response.Status.CREATED).entity(createdDTO).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateSpaceMarine(@PathParam("id") Integer id, @Valid SpaceMarineDTO spaceMarineDTO) {
        SpaceMarine spaceMarine = spaceMarineMapper.toEntity(spaceMarineDTO);
        SpaceMarine updatedSpaceMarine = spaceMarineService.updateSpaceMarine(id, spaceMarine);
        if (updatedSpaceMarine != null) {
            SpaceMarineDTO updatedDTO = spaceMarineMapper.toDTO(updatedSpaceMarine);
            return Response.ok(updatedDTO).build();
        } else {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSpaceMarine(@PathParam("id") Integer id) {
        boolean deleted = spaceMarineService.deleteSpaceMarine(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            throw new EntityNotFoundException("SpaceMarine", id);
        }
    }

    @GET
    @Path("/search/name")
    public Response findByNameContaining(@QueryParam("name") String name) {
        List<SpaceMarine> spaceMarines = spaceMarineService.findSpaceMarinesByNameContaining(name);
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        return Response.ok(spaceMarineDTOs).build();
    }

    @GET
    @Path("/search/health")
    public Response findByHealthLessThan(@QueryParam("health") Integer health) {
        List<SpaceMarine> spaceMarines = spaceMarineService.findSpaceMarinesByHealthLessThan(health);
        List<SpaceMarineDTO> spaceMarineDTOs = spaceMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        return Response.ok(spaceMarineDTOs).build();
    }

    @GET
    @Path("/count/health")
    public Response countByHealthLessThan(@QueryParam("health") Integer health) {
        long count = spaceMarineService.countSpaceMarinesByHealthLessThan(health);
        return Response.ok(new CountResponse(count)).build();
    }

    @GET
    @Path("/stats/average-heart-count")
    public Response getAverageHeartCount() {
        Double average = spaceMarineService.getAverageHeartCount();
        return Response.ok(new AverageResponse(average)).build();
    }

}
