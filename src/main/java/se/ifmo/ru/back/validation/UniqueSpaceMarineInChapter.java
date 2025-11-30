package se.ifmo.ru.back.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueSpaceMarineInChapterValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueSpaceMarineInChapter {
    String message() default "Десантник с таким здоровьем, оружием и координатами уже существует в этой главе";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

