package com.photobogota.api.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Estadísticas agregadas de los locales de un SOCIO para un período
 * (semana, mes o año): vistas, reseñas, calificación promedio y uso de
 * promociones.
 */
@Data
@Schema(description = "Estadísticas del socio para el período solicitado")
public class EstadisticasSocioDTO {

    @Schema(description = "Período calculado", example = "mes")
    private String periodo;

    @Schema(description = "Indicadores principales del período")
    private KpisDTO kpis;

    @Schema(description = "Serie temporal de visitas")
    private List<PuntoSerieDTO> seriesVisitas = new ArrayList<>();

    @Schema(description = "Conteo de reseñas por cantidad de estrellas")
    private List<DistribucionResenaDTO> distribucionResenas = new ArrayList<>();

    @Schema(description = "Top de locales del socio por visitas en el período")
    private List<LugarPopularDTO> lugaresPopulares = new ArrayList<>();

    @Schema(description = "Serie temporal de usos de promociones (canjes)")
    private List<PuntoSerieDTO> seriesUsosPromociones = new ArrayList<>();

    @Schema(description = "true si el socio tiene promociones registradas")
    private boolean hayPromociones;

    @Data
    @Schema(description = "Métricas principales")
    public static class KpisDTO {
        private long visitas;
        private double visitasCambio;
        private long resenas;
        private double resenasCambio;
        private double calificacionPromedio;
        private double calificacionCambio;
        private long usosPromociones;
        private double usosPromocionesCambio;
    }

    @Data
    @Schema(description = "Punto de una serie temporal")
    public static class PuntoSerieDTO {
        private String etiqueta;
        private long valor;

        public PuntoSerieDTO() {
        }

        public PuntoSerieDTO(String etiqueta, long valor) {
            this.etiqueta = etiqueta;
            this.valor = valor;
        }
    }

    @Data
    @Schema(description = "Conteo de reseñas por estrellas")
    public static class DistribucionResenaDTO {
        private int estrellas;
        private long cantidad;
    }

    @Data
    @Schema(description = "Local del socio con sus visitas en el período")
    public static class LugarPopularDTO {
        private String id;
        private String nombre;
        private long visitas;
        private double rating;
        private String imagen;
    }
}