package com.photobogota.api.model;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registra el ciclo de vida completo de una solicitud de autoeliminación de
 * cuenta hecha por un MIEMBRO: motivo, verificación por código, período de
 * recuperación de 30 días y resultado final (cancelada o completada).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "solicitudes_eliminacion")
public class SolicitudEliminacionCuenta {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId usuarioId;

    private String email;

    private String nombreUsuario;

    private MotivoEliminacionCuenta motivo;

    private String comentario;

    private EstadoSolicitudEliminacion estado;

    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaConfirmacion;

    private LocalDateTime fechaProgramadaEliminacion;

    private LocalDateTime fechaCancelacion;

    private LocalDateTime fechaCompletada;

    // ── Campos de procesamiento administrativo (Etapa 2) ──

    // true si un ADMIN forzó el procesamiento (anonimización inmediata,
    // sin esperar los 30 días) en lugar del job automático.
    @Builder.Default
    private Boolean procesadaManualmente = false;

    // nombreUsuario del administrador que procesó o rechazó la solicitud
    private String procesadaPorAdmin;

    // Nota interna dejada por el administrador al procesar/rechazar
    private String observacionAdmin;
}
