package se.ifmo.ru.back.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.repository.SpaceMarineRepository;

import java.util.List;

@Component
public class UniqueSpaceMarineInChapterValidator implements ConstraintValidator<UniqueSpaceMarineInChapter, SpaceMarine> {

    @Autowired
    private SpaceMarineRepository spaceMarineRepository;

    @Override
    public void initialize(UniqueSpaceMarineInChapter constraintAnnotation) {
    }

    @Override
    public boolean isValid(SpaceMarine spaceMarine, ConstraintValidatorContext context) {
        if (spaceMarine == null || spaceMarine.getHealth() == null || spaceMarine.getCoordinates() == null) {
            return true; // Базовая валидация
        }

        // Если глава не указана, проверяем уникальность без учета главы
        if (spaceMarine.getChapter() == null || spaceMarine.getChapter().getId() == null) {
            return true;
        }

        Long chapterId = spaceMarine.getChapter().getId();

        // Получаем все десантники в этой главе
        List<SpaceMarine> marinesInChapter = spaceMarineRepository.findByChapterId(chapterId);

        // Проверяем уникальность комбинации: health + weaponType + coordinates
        return marinesInChapter.stream()
                .noneMatch(marine -> {
                    // Пропускаем сам объект при проверке
                    if (marine.getId() != null && spaceMarine.getId() != null && 
                        marine.getId().equals(spaceMarine.getId())) {
                        return false;
                    }
                    
                    boolean healthMatch = marine.getHealth().equals(spaceMarine.getHealth());
                    
                    // Сравнение weaponType (может быть null)
                    boolean weaponMatch = (marine.getWeaponType() == null && spaceMarine.getWeaponType() == null) ||
                                        (marine.getWeaponType() != null && spaceMarine.getWeaponType() != null &&
                                         marine.getWeaponType().equals(spaceMarine.getWeaponType()));
                    
                    // Сравнение coordinates
                    boolean coordsMatch = marine.getCoordinates() != null && 
                                         spaceMarine.getCoordinates() != null &&
                                         marine.getCoordinates().getId().equals(spaceMarine.getCoordinates().getId());
                    
                    return healthMatch && weaponMatch && coordsMatch;
                });
    }
}

