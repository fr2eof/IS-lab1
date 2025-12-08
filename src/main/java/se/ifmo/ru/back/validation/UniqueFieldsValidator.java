package se.ifmo.ru.back.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@Component
public class UniqueFieldsValidator implements ConstraintValidator<UniqueFields, Object> {

    @Autowired
    private UniqueFieldsRepositoryProvider repositoryProvider;

    private String[] fields;
    private String idField;

    @Override
    public void initialize(UniqueFields constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
        this.idField = constraintAnnotation.idField();
    }

    @Override
    public boolean isValid(Object entity, ConstraintValidatorContext context) {
        if (entity == null) {
            return true;
        }

        // Если repositoryProvider не инициализирован (создан не через Spring), пропускаем валидацию
        if (repositoryProvider == null) {
            return true;
        }

        try {
            // Получаем репозиторий для типа сущности
            JpaRepository<?, ?> repository = repositoryProvider.getRepository(entity.getClass());
            if (repository == null) {
                return true; // Если репозиторий не найден, пропускаем проверку
            }

            // Получаем ID текущего объекта (для исключения при обновлении)
            Object currentId = getFieldValue(entity, idField);

            // Получаем значения полей для проверки уникальности
            Object[] fieldValues = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                fieldValues[i] = getFieldValue(entity, fields[i]);
            }

            // Проверяем уникальность среди всех объектов в репозитории
            List<?> allEntities = repository.findAll();
            return allEntities.stream()
                    .noneMatch(existingEntity -> {
                        // Исключаем сам объект при обновлении
                        Object existingId = getFieldValue(existingEntity, idField);
                        if (currentId != null && existingId != null && 
                            Objects.equals(currentId, existingId)) {
                            return false;
                        }

                        // Проверяем совпадение всех полей
                        for (int i = 0; i < fields.length; i++) {
                            Object existingValue = getFieldValue(existingEntity, fields[i]);
                            if (!Objects.equals(fieldValues[i], existingValue)) {
                                return false;
                            }
                        }
                        return true;
                    });

        } catch (Exception e) {
            // В случае ошибки пропускаем валидацию
            return true;
        }
    }

    private Object getFieldValue(Object entity, String fieldName) {
        try {
            // Пробуем получить значение через геттер
            String getterName = "get" + capitalize(fieldName);
            Method getter = entity.getClass().getMethod(getterName);
            return getter.invoke(entity);
        } catch (Exception e) {
            try {
                // Если геттер не найден, пробуем получить через поле напрямую
                Field field = entity.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(entity);
                // Для примитивных типов возвращаем как объект
                if (field.getType().isPrimitive()) {
                    if (field.getType() == int.class) {
                        return (Integer) value;
                    } else if (field.getType() == long.class) {
                        return (Long) value;
                    } else if (field.getType() == double.class) {
                        return (Double) value;
                    } else if (field.getType() == float.class) {
                        return (Float) value;
                    } else if (field.getType() == boolean.class) {
                        return (Boolean) value;
                    }
                }
                return value;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

