package com.photobogota.api.mapper;

import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.SpotResponseDTO;
import com.photobogota.api.model.Spot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SpotMapper {

    // Spot → SpotResumenDTO (para los marcadores del mapa)
    @Mapping(target = "imagen", expression = "java(spot.getImagenes().isEmpty() ? null : spot.getImagenes().get(0))")
    SpotResumenDTO toResumen(Spot spot);

    // Lista de spots → Lista de resúmenes
    List<SpotResumenDTO> toResumenList(List<Spot> spots);

    // Spot → SpotResponseDTO (para el detalle completo)
    @Mapping(target = "imagen", expression = "java(spot.getImagenes().isEmpty() ? null : spot.getImagenes().get(0))")
    @Mapping(target = "resenas", source = "resenas")
    SpotResponseDTO toResponse(Spot spot);

    // Spot.Resena → SpotResponseDTO.ResenaResponseDTO
    @Mapping(target = "fecha", expression = "java(formatearFecha(resena.getFecha()))")
    SpotResponseDTO.ResenaResponseDTO toResenaResponse(Spot.Resena resena);

    // MapStruct detecta este método automáticamente para mapear la lista de reseñas
    List<SpotResponseDTO.ResenaResponseDTO> toResenaResponseList(List<Spot.Resena> resenas);

    // Lógica de formato de fecha — igual a como lo manejas con toEntity en UsuarioMapper
    default String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) return "Recientemente";
        long dias = ChronoUnit.DAYS.between(fecha, LocalDateTime.now());
        if (dias == 0)   return "Hoy";
        if (dias == 1)   return "Ayer";
        if (dias < 7)    return "Hace " + dias + " días";
        if (dias < 30)   return "Hace " + (dias / 7) + " semanas";
        if (dias < 365)  return "Hace " + (dias / 30) + " meses";
        return "Hace " + (dias / 365) + " años";
    }
}