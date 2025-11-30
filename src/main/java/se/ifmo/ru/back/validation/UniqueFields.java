package se.ifmo.ru.back.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueFieldsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueFields {
    String message() default "Объект с такими значениями полей уже существует";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Имена полей, по которым проверяется уникальность
     */
    String[] fields();
    
    /**
     * Имя поля с ID (для исключения текущего объекта при обновлении)
     */
    String idField() default "id";
}

