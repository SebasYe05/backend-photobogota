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
    private String fotoPerfil;

    @Builder.Default
    private Boolean estadoCuenta = true;
    
    private String biografia;
    
    @Builder.Default
    private Boolean correoConfirmado = false;
    
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    // Conteo de infracciones detectadas por el filtro automático de contenido.
    // Determina la sanción progresiva: 1 notificación, 2 mute, 3 suspensión, 4+ ban.
    @Builder.Default
    private Integer contadorInfracciones = 0;

    // Sanción actualmente activa (si existe). Se usa para bloquear publicaciones
    // y para que el frontend informe al usuario sobre su estado.
    private Sancion sancion;
}
