package com.photobogota.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "promociones")
public class Promocion {

    @Id
    private String id;

    // Id del "local" (spot creado por un SOCIO) al que pertenece la promoción.
    @Indexed
    private String spotId;

    // nombreUsuario del socio dueño del local, denormalizado desde el JWT.
    @Indexed
    private String socioUsername;

    // Nombre del local denormalizado para mostrarlo sin hacer join.
    private String nombreSpot;

    private String titulo;
    private String descripcion;

    // Tipo de promoción: descuento, regalos, pack, demostracion, gratis, otro...
    private String tipo;

    // Ej: "50%" o "30% OFF" (opcional)
    private String descuento;

    // Código promocional para canjear (opcional)
    private String codigo;

    // URLs de imágenes ya subidas (via /api/v1/imagenes/spot)
    @Builder.Default
    private List<String> imagenes = new ArrayList<>();

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    // false cuando el socio la desactiva manualmente (deja de mostrarse al público).
    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    private Integer usos = 0;

    // null = ilimitado
    private Integer usosMaximos;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}