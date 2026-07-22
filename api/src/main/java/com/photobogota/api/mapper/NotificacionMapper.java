package com.photobogota.api.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.model.Notificacion;

@Mapper(componentModel = "spring")
public interface NotificacionMapper {

    // MapStruct mapea el enum "tipo" a String automáticamente usando su .name()
    NotificacionResponseDTO toResponse(Notificacion notificacion);

    List<NotificacionResponseDTO> toResponseList(List<Notificacion> notificaciones);
}
