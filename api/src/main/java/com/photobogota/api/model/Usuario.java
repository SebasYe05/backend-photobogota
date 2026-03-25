package com.photobogota.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "usuarios")
public abstract class Usuario { // Hacemos la clase abstracta para que no se pueda instanciar directamente
    private ObjectId id;
    private String nombresCompletos;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String fotoPefil;

    @Builder.Default
    private Boolean estadoCuenta = true;
    
    private String biografia;
    
    @Builder.Default
    private Boolean correoConfirmado = false;
    
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
