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
 * Canje de una promoción realizado por un MIEMBRO y validado por el SOCIO
 * dueño del local. El estado EXPIRADO se deduce al leer (fechaExpiracion ya
 * pasada y aún no usado); aquí solo se persisten VIGENTE y USADO.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "canjes")
public class Canje {

    @Id
    private String id;

    // Código de 8 caracteres alfanuméricos generado al crear el canje.
    @Indexed(unique = true)
    private String codigo;

    // VIGENTE | USADO
    private String estado;

    private LocalDateTime fechaCanje;

    // = promocion.fechaFin en el momento del canje.
    private LocalDateTime fechaExpiracion;

    @Indexed
    private String promocionId;

    // Denormalizados para no hacer joins al listar.
    private String promocionTitulo;
    private String descuento;

    @Indexed
    private String spotId;
    private String spotNombre;

    @Indexed
    private String miembroNombre;

    // nombreUsuario del socio dueño del local, denormalizado desde el JWT.
    @Indexed
    private String socioUsername;
}