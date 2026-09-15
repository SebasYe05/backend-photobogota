package com.photobogota.api.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.photobogota.api.dto.CanjeResponseDTO;
import com.photobogota.api.model.Canje;

@Mapper(componentModel = "spring")
public interface CanjeMapper {

    @Mapping(target = "estado", expression = "java(calcularEstado(canje))")
    CanjeResponseDTO toResponse(Canje canje);

    List<CanjeResponseDTO> toResponseList(List<Canje> canjes);

    default String calcularEstado(Canje canje) {
        if (canje == null || "USADO".equals(canje.getEstado())) {
            return "USADO";
        }
        LocalDateTime expiracion = canje.getFechaExpiracion();
        if (expiracion != null && expiracion.isBefore(LocalDateTime.now())) {
            return "EXPIRADO";
        }
        return "VIGENTE";
    }
}