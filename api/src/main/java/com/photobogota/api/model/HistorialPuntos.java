package com.photobogota.api.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "historial_puntos")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistorialPuntos {

    @Id
    private String id;

    private String usuario;

    private TipoPuntos tipo;

    private Integer puntos;

    private String refId;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    private String motivo;
}
