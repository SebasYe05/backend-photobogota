package com.photobogota.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Registro de una visita al detalle de un local/spot. Se guarda con la fecha
 * exacta para poder armar tendencias por día, mes o año en el panel de
 * estadísticas del socio. Las visitas hechas por el propio dueño no se
 * contabilizan.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "vistas_spots")
public class VistaSpot {

    @Id
    private String id;

    @Indexed
    private String spotId;

    // nombreUsuario del visitante (null si es un visitante anónimo).
    private String usuario;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}