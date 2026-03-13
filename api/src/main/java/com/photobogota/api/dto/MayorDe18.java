package com.photobogota.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MayorDe18Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDe18 {
    String message() default "Debes ser mayor de 18 años para registrarte";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
