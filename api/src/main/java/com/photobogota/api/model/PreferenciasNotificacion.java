package com.photobogota.api.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preferencias de notificaciones configuradas por un usuario.
 * Si un usuario no tiene documento aquí, se asumen los valores por defecto
 * (notificaciones activas, canal APP, sin zonas/categorías de interés configuradas).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "preferencias_notificaciones")
public class PreferenciasNotificacion {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username; // nombreUsuario dueño de las preferencias

    @Builder.Default
    private Boolean notificacionesActivas = true;

    @Builder.Default
    private CanalNotificacion canalPreferido = CanalNotificacion.APP;

    @Builder.Default
    private List<NotificacionTipo> tiposSilenciados = new ArrayList<>();

    // Localidades de interés para alertar sobre spots nuevos (ej: "Chapinero", "Usaquén")
    @Builder.Default
    private List<String> localidadesInteres = new ArrayList<>();

    // Categorías de interés para alertar sobre spots nuevos (ej: "Paisaje urbano")
    @Builder.Default
    private List<String> categoriasInteres = new ArrayList<>();
}
