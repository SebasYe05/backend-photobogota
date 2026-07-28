package com.photobogota.api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request del Admin para programar una ventana de mantenimiento.
 * Al guardarse, dispara automáticamente el aviso a todos los usuarios
 * (reutilizando el mismo servicio genérico de envío de notificaciones).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramarMantenimientoRequestDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser en el futuro")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaFin;

    @NotBlank(message = "Debes indicar el motivo del mantenimiento")
    private String motivo;

    // Opcional: si no se envía, se genera un mensaje automático con las fechas y el motivo.
    private String mensajePersonalizado;
}
