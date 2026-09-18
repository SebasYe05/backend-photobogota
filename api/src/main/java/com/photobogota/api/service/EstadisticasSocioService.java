package com.photobogota.api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.EstadisticasSocioDTO;
import com.photobogota.api.model.Canje;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.VistaSpot;
import com.photobogota.api.repository.CanjeRepository;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.VistaSpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agrega las métricas de los locales de un SOCIO (visitas, reseñas,
 * calificación promedio y usos de promociones) para los períodos
 * semana / mes / año.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstadisticasSocioService {

    private static final String[] DIAS_SEMANA = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
    private static final String[] MESES = { "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic" };

    private final SpotRepository spotRepository;
    private final VistaSpotRepository vistaSpotRepository;
    private final CanjeRepository canjeRepository;
    private final PromocionRepository promocionRepository;

    public EstadisticasSocioDTO obtenerEstadisticas(String socioUsername, String periodo) {
        String periodoNormalizado = normalizarPeriodo(periodo);
        LocalDateTime ahora = LocalDateTime.now();
        Rango actual = rangoActual(periodoNormalizado, ahora);
        Rango anterior = rangoAnterior(periodoNormalizado, ahora);

        List<Spot> spots = spotRepository.findByCreadorUsername(socioUsername);
        List<String> spotIds = spots.stream()
                .map(Spot::getId)
                .filter(Objects::nonNull)
                .toList();

        EstadisticasSocioDTO dto = new EstadisticasSocioDTO();
        dto.setPeriodo(periodoNormalizado);

        // --- KPIs ---
        long visitas = contarVistas(spotIds, actual);
        long visitasAnteriores = contarVistas(spotIds, anterior);
        long resenas = contarResenas(spots, actual);
        long resenasAnteriores = contarResenas(spots, anterior);
        double calificacion = promediarResenas(spots, actual);
        double calificacionAnterior = promediarResenas(spots, anterior);

        List<Canje> canjesActuales = obtenerCanjes(socioUsername, actual);
        List<Canje> canjesAnteriores = obtenerCanjes(socioUsername, anterior);

        EstadisticasSocioDTO.KpisDTO kpis = new EstadisticasSocioDTO.KpisDTO();
        kpis.setVisitas(visitas);
        kpis.setVisitasCambio(cambioPorcentual(visitas, visitasAnteriores));
        kpis.setResenas(resenas);
        kpis.setResenasCambio(cambioPorcentual(resenas, resenasAnteriores));
        kpis.setCalificacionPromedio(redondear(calificacion));
        kpis.setCalificacionCambio(redondear(calificacion - calificacionAnterior));
        kpis.setUsosPromociones(canjesActuales.size());
        kpis.setUsosPromocionesCambio(cambioPorcentual(canjesActuales.size(), canjesAnteriores.size()));
        dto.setKpis(kpis);

        // --- Series temporales ---
        List<VistaSpot> vistasActuales = obtenerVistas(spotIds, actual);
        dto.setSeriesVisitas(serieTemporal(
                vistasActuales.stream().map(VistaSpot::getFecha).filter(Objects::nonNull).toList(),
                periodoNormalizado, ahora));
        dto.setSeriesUsosPromociones(serieTemporal(
                canjesActuales.stream().map(Canje::getFechaCanje).filter(Objects::nonNull).toList(),
                periodoNormalizado, ahora));

        // --- Distribución de estrellas y lugares populares ---
        dto.setDistribucionResenas(distribucionResenas(spots, actual));
        dto.setLugaresPopulares(lugaresPopulares(spots, obtenerVistas(spotIds, actual), actual));

        dto.setHayPromociones(!promocionRepository.findBySocioUsername(socioUsername).isEmpty());

        return dto;
    }

    // ------------------------------------------------------------------
    // Rangos de fechas por período
    // ------------------------------------------------------------------

    private String normalizarPeriodo(String periodo) {
        if ("ano".equalsIgnoreCase(periodo) || "año".equalsIgnoreCase(periodo)) {
            return "ano";
        }
        if ("semana".equalsIgnoreCase(periodo)) {
            return "semana";
        }
        return "mes";
    }

    private Rango rangoActual(String periodo, LocalDateTime ahora) {
        switch (periodo) {
            case "semana":
                LocalDate lunes = ahora.toLocalDate().with(DayOfWeek.MONDAY);
                return new Rango(lunes.atStartOfDay(), ahora);
            case "ano":
                LocalDate inicioAno = LocalDate.of(ahora.getYear(), 1, 1);
                return new Rango(inicioAno.atStartOfDay(), ahora);
            case "mes":
            default:
                LocalDate inicioMes = YearMonth.from(ahora).atDay(1);
                return new Rango(inicioMes.atStartOfDay(), ahora);
        }
    }

    private Rango rangoAnterior(String periodo, LocalDateTime ahora) {
        switch (periodo) {
            case "semana":
                LocalDate lunes = ahora.toLocalDate().with(DayOfWeek.MONDAY);
                return new Rango(lunes.minusWeeks(1).atStartOfDay(), lunes.atStartOfDay());
            case "ano":
                LocalDate inicioAno = LocalDate.of(ahora.getYear(), 1, 1);
                return new Rango(inicioAno.minusYears(1).atStartOfDay(), inicioAno.atStartOfDay());
            case "mes":
            default:
                LocalDate inicioMes = YearMonth.from(ahora).atDay(1);
                return new Rango(YearMonth.from(inicioMes).minusMonths(1).atDay(1).atStartOfDay(),
                        inicioMes.atStartOfDay());
        }
    }

    // ------------------------------------------------------------------
    // Conteos
    // ------------------------------------------------------------------

    private List<VistaSpot> obtenerVistas(List<String> spotIds, Rango rango) {
        if (spotIds.isEmpty()) {
            return List.of();
        }
        return vistaSpotRepository.findBySpotIdInAndFechaBetween(spotIds, rango.desde, rango.hasta);
    }

    private long contarVistas(List<String> spotIds, Rango rango) {
        return obtenerVistas(spotIds, rango).size();
    }

    private List<Canje> obtenerCanjes(String socioUsername, Rango rango) {
        return canjeRepository.findBySocioUsernameAndFechaCanjeBetween(socioUsername, rango.desde, rango.hasta);
    }

    private long contarResenas(List<Spot> spots, Rango rango) {
        return resenasEnRango(spots, rango).size();
    }

    private double promediarResenas(List<Spot> spots, Rango rango) {
        return resenasEnRango(spots, rango).stream()
                .mapToInt(Spot.Resena::getRating)
                .average()
                .orElse(0.0);
    }

    private List<Spot.Resena> resenasEnRango(List<Spot> spots, Rango rango) {
        List<Spot.Resena> resenas = new ArrayList<>();
        for (Spot spot : spots) {
            if (spot.getResenas() == null) {
                continue;
            }
            for (Spot.Resena resena : spot.getResenas()) {
                LocalDateTime fecha = resena.getFecha();
                if (fecha != null && !fecha.isBefore(rango.desde) && !fecha.isAfter(rango.hasta)) {
                    resenas.add(resena);
                }
            }
        }
        return resenas;
    }

    private List<EstadisticasSocioDTO.DistribucionResenaDTO> distribucionResenas(List<Spot> spots, Rango rango) {
        Map<Integer, Long> conteo = new HashMap<>();
        for (Spot.Resena resena : resenasEnRango(spots, rango)) {
            int estrellas = resena.getRating() != null ? resena.getRating() : 0;
            conteo.merge(estrellas, 1L, Long::sum);
        }

        List<EstadisticasSocioDTO.DistribucionResenaDTO> lista = new ArrayList<>();
        for (int estrellas = 5; estrellas >= 1; estrellas--) {
            EstadisticasSocioDTO.DistribucionResenaDTO item = new EstadisticasSocioDTO.DistribucionResenaDTO();
            item.setEstrellas(estrellas);
            item.setCantidad(conteo.getOrDefault(estrellas, 0L));
            lista.add(item);
        }
        return lista;
    }

    private List<EstadisticasSocioDTO.LugarPopularDTO> lugaresPopulares(List<Spot> spots,
            List<VistaSpot> vistasEnRango, Rango rango) {
        Map<String, Long> vistasPorSpot = new HashMap<>();
        for (VistaSpot vista : vistasEnRango) {
            if (vista.getFecha() != null && !vista.getFecha().isBefore(rango.desde)
                    && !vista.getFecha().isAfter(rango.hasta)) {
                vistasPorSpot.merge(vista.getSpotId(), 1L, Long::sum);
            }
        }

        List<EstadisticasSocioDTO.LugarPopularDTO> lugares = new ArrayList<>();
        for (Spot spot : spots) {
            long visitas = vistasPorSpot.getOrDefault(spot.getId(), 0L);
            lugares.add(construirLugarPopular(spot, visitas));
        }

        lugares.sort((a, b) -> {
            int porVisitas = Long.compare(b.getVisitas(), a.getVisitas());
            if (porVisitas != 0) {
                return porVisitas;
            }
            return Double.compare(b.getRating(), a.getRating());
        });

        return lugares.stream().limit(5).toList();
    }

    private EstadisticasSocioDTO.LugarPopularDTO construirLugarPopular(Spot spot, long visitas) {
        EstadisticasSocioDTO.LugarPopularDTO lugar = new EstadisticasSocioDTO.LugarPopularDTO();
        lugar.setId(spot.getId());
        lugar.setNombre(spot.getNombre());
        lugar.setVisitas(visitas);
        lugar.setRating(spot.getRating() != null ? spot.getRating() : 0.0);
        lugar.setImagen(spot.getImagenes() != null && !spot.getImagenes().isEmpty()
                ? spot.getImagenes().get(0)
                : null);
        return lugar;
    }

    // ------------------------------------------------------------------
    // Series temporales
    // ------------------------------------------------------------------

    private List<EstadisticasSocioDTO.PuntoSerieDTO> serieTemporal(List<LocalDateTime> fechas, String periodo,
            LocalDateTime ahora) {
        int totalBuckets;
        switch (periodo) {
            case "semana":
                totalBuckets = 7;
                break;
            case "ano":
                totalBuckets = 12;
                break;
            case "mes":
            default:
                totalBuckets = ahora.getDayOfMonth();
                break;
        }

        long[] valores = new long[totalBuckets];
        for (LocalDateTime fecha : fechas) {
            int indice = indiceBucket(fecha, periodo);
            if (indice >= 0 && indice < totalBuckets) {
                valores[indice]++;
            }
        }

        List<EstadisticasSocioDTO.PuntoSerieDTO> serie = new ArrayList<>();
        for (int i = 0; i < totalBuckets; i++) {
            serie.add(new EstadisticasSocioDTO.PuntoSerieDTO(etiquetaBucket(i, periodo, ahora), valores[i]));
        }
        return serie;
    }

    private int indiceBucket(LocalDateTime fecha, String periodo) {
        switch (periodo) {
            case "semana":
                LocalDate lunes = fecha.toLocalDate().with(DayOfWeek.MONDAY);
                return (int) ChronoUnit.DAYS.between(lunes, fecha.toLocalDate());
            case "ano":
                return fecha.getMonthValue() - 1;
            case "mes":
            default:
                return fecha.getDayOfMonth() - 1;
        }
    }

    private String etiquetaBucket(int indice, String periodo, LocalDateTime ahora) {
        switch (periodo) {
            case "semana":
                return DIAS_SEMANA[indice];
            case "ano":
                return MESES[indice];
            case "mes":
            default:
                return String.valueOf(indice + 1);
        }
    }

    private double cambioPorcentual(long actual, long anterior) {
        if (anterior == 0) {
            return actual == 0 ? 0.0 : 100.0;
        }
        return redondear((actual - anterior) * 100.0 / anterior);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private static class Rango {
        private final LocalDateTime desde;
        private final LocalDateTime hasta;

        private Rango(LocalDateTime desde, LocalDateTime hasta) {
            this.desde = desde;
            this.hasta = hasta;
        }
    }
}