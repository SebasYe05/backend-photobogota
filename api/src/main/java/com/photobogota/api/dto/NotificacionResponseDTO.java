package com.photobogota.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionResponseDTO {

    private String id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String spotId;
    private String emisorUsername;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
}
