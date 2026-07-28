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
 * Ventana de mantenimiento programada por un Admin (HU #47).
 * Mientras "ahora" esté entre fechaInicio y fechaFin (y no esté cancelada),
 * el MantenimientoFilter bloquea las peticiones y responde 503.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "mantenimientos_programados")
public class MantenimientoProgramado {

    @Id
    private String id;

    @Indexed
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private String mensaje;

    private String motivo;

    private String creadoPor; // nombreUsuario del admin que programó el mantenimiento

    @Builder.Default
    private Boolean cancelado = false;

    // Usados por el scheduler para no reenviar el mismo aviso de inicio/fin varias veces
    @Builder.Default
    private Boolean avisoInicioEnviado = false;

    @Builder.Default
    private Boolean avisoFinEnviado = false;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
