package com.photobogota.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "guardados")
public class Guardado {

    @Id
    private String id;

    private String nombreUsuario;

    private String spotId;

    private LocalDateTime fecha = LocalDateTime.now();
}