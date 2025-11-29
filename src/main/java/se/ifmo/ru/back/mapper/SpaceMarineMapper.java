package se.ifmo.ru.back.mapper;

import org.springframework.stereotype.Component;
import se.ifmo.ru.back.dto.*;
import se.ifmo.ru.back.entity.*;

@Component
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
            entity.getWeaponType() != null ? entity.getWeaponType().name() : null,
            null, // coordinatesId - не используется при чтении
            entity.getChapter() != null ? entity.getChapter().getId() : null
        );
    }

    public SpaceMarine toEntity(SpaceMarineDTO dto) {
        if (dto == null) {
            return null;
        }
        
        SpaceMarine entity = new SpaceMarine();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setCoordinates(toCoordinatesEntity(dto.coordinates()));
        entity.setCreationDate(dto.creationDate());
        entity.setChapter(toChapterEntity(dto.chapter()));
        entity.setHealth(dto.health());
        entity.setHeartCount(dto.heartCount());
        
        if (dto.category() != null) {
            entity.setCategory(AstartesCategory.valueOf(dto.category()));
        }
        
        if (dto.weaponType() != null) {
            entity.setWeaponType(Weapon.valueOf(dto.weaponType()));
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
        entity.setId(dto.id());
        entity.setX(dto.x());
        entity.setY(dto.y());
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
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setMarinesCount(dto.marinesCount());
        return entity;
    }
}
