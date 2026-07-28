package com.photobogota.api.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notificación individual entregada a un usuario (bandeja "in-app").
 * Cada destinatario tiene su propia copia del documento para poder
 * llevar su propio estado de "leída" y para poder filtrar/paginar por usuario.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "notificaciones")
public class Notificacion {

    public static final String FIELD_FECHA_CREACION = "fechaCreacion";

    @Id
    private String id;

    @Indexed
    private String destinatarioUsername; // nombreUsuario del que la recibe

    private NotificacionTipo tipo;

    private String titulo;

    private String mensaje;

    private String spotId; // opcional: referencia al spot relacionado (si aplica)

    private String emisorUsername; // "sistema" o nombreUsuario del admin/moderador que la envió

    @Builder.Default
    private Boolean leida = false;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
