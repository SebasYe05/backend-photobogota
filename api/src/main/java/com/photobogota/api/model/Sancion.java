package com.photobogota.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sancion {

    private TipoSancion tipo;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaExpiracion;

    private String motivo;

    private String impuestaPor;
}
