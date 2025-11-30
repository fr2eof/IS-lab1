package se.ifmo.ru.back.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.entity.Coordinates;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.repository.ChapterRepository;
import se.ifmo.ru.back.repository.CoordinatesRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;

import java.util.HashMap;
import java.util.Map;

@Component
public class UniqueFieldsRepositoryProvider {

    private final Map<Class<?>, JpaRepository<?, ?>> repositoryMap = new HashMap<>();

    @Autowired
    public UniqueFieldsRepositoryProvider(
            CoordinatesRepository coordinatesRepository,
            ChapterRepository chapterRepository,
            SpaceMarineRepository spaceMarineRepository) {
        repositoryMap.put(Coordinates.class, coordinatesRepository);
        repositoryMap.put(Chapter.class, chapterRepository);
        repositoryMap.put(SpaceMarine.class, spaceMarineRepository);
    }

    @SuppressWarnings("unchecked")
    public <T> JpaRepository<T, ?> getRepository(Class<T> entityClass) {
        return (JpaRepository<T, ?>) repositoryMap.get(entityClass);
    }
}

