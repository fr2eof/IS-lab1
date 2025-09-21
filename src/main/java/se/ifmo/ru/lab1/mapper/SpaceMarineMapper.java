package se.ifmo.ru.lab1.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import se.ifmo.ru.lab1.dto.*;
import se.ifmo.ru.lab1.entity.*;

@ApplicationScoped
public class SpaceMarineMapper {

    public SpaceMarineDTO toDTO(SpaceMarine entity) {
        if (entity == null) {
            return null;
        }
        
        return new SpaceMarineDTO(
            entity.getId(),
            entity.getName(),
            toCoordinatesDTO(entity.getCoordinates()),
            entity.getCreationDate(),
            toChapterDTO(entity.getChapter()),
            entity.getHealth(),
            entity.getHeartCount(),
            entity.getCategory() != null ? entity.getCategory().name() : null,
            entity.getWeaponType() != null ? entity.getWeaponType().name() : null
        );
    }

    public SpaceMarine toEntity(SpaceMarineDTO dto) {
        if (dto == null) {
            return null;
        }
        
        SpaceMarine entity = new SpaceMarine();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCoordinates(toCoordinatesEntity(dto.getCoordinates()));
        entity.setCreationDate(dto.getCreationDate());
        entity.setChapter(toChapterEntity(dto.getChapter()));
        entity.setHealth(dto.getHealth());
        entity.setHeartCount(dto.getHeartCount());
        
        if (dto.getCategory() != null) {
            entity.setCategory(AstartesCategory.valueOf(dto.getCategory()));
        }
        
        if (dto.getWeaponType() != null) {
            entity.setWeaponType(Weapon.valueOf(dto.getWeaponType()));
        }
        
        return entity;
    }

    public CoordinatesDTO toCoordinatesDTO(Coordinates entity) {
        if (entity == null) {
            return null;
        }
        
        return new CoordinatesDTO(
            entity.getId(),
            entity.getX(),
            entity.getY()
        );
    }

    public Coordinates toCoordinatesEntity(CoordinatesDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Coordinates entity = new Coordinates();
        entity.setId(dto.getId());
        entity.setX(dto.getX());
        entity.setY(dto.getY());
        return entity;
    }

    public ChapterDTO toChapterDTO(Chapter entity) {
        if (entity == null) {
            return null;
        }
        
        return new ChapterDTO(
            entity.getId(),
            entity.getName(),
            entity.getMarinesCount()
        );
    }

    public Chapter toChapterEntity(ChapterDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Chapter entity = new Chapter();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setMarinesCount(dto.getMarinesCount());
        return entity;
    }
}
