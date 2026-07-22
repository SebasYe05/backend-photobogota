package com.photobogota.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "calificaciones")
public class Calificacion {

    @Id
    private String id;

    private String spotId;

    private String usuario;

    private Integer estrellas;

    private String comentario;

    private LocalDateTime fecha = LocalDateTime.now();
}
