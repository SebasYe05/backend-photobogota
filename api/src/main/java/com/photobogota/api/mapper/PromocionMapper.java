package com.photobogota.api.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.model.Promocion;

@Mapper(componentModel = "spring")
public interface PromocionMapper {

    @Mapping(target = "imagen", expression = "java(promocion.getImagenes().isEmpty() ? null : promocion.getImagenes().get(0))")
    @Mapping(target = "estado", expression = "java(calcularEstado(promocion))")
    PromocionResponseDTO toResponse(Promocion promocion);

    List<PromocionResponseDTO> toResponseList(List<Promocion> promociones);

    default String calcularEstado(Promocion promocion) {
        if (!Boolean.TRUE.equals(promocion.getActivo())) {
            return "DESACTIVADA";
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = promocion.getFechaInicio();
        LocalDateTime fin = promocion.getFechaFin();
        if (fin != null && fin.isBefore(ahora)) {
            return "EXPIRADA";
        }
        if (inicio != null && inicio.isAfter(ahora)) {
            return "PROXIMA";
        }
        return "ACTIVA";
    }
}