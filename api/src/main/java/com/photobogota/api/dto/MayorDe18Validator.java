package com.photobogota.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MayorDe18Validator implements ConstraintValidator<MayorDe18, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        
        // Calculamos la diferencia en AÑOS entre la fecha de nacimiento y hoy
        long edad = ChronoUnit.YEARS.between(fechaNacimiento, LocalDate.now());
        
        return edad >= 18;
    }
}