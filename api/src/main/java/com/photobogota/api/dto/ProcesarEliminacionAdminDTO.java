package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Observación opcional que el administrador deja al procesar o rechazar una solicitud de eliminación")
public class ProcesarEliminacionAdminDTO {

    @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
    private String observacion;
}
