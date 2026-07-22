package com.photobogota.api.dto;

import java.util.List;

import com.photobogota.api.model.AlcanceNotificacion;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.model.Rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para que un Admin o Moderador dispare una notificación manual
 * (anuncio) hacia todos los usuarios, hacia uno o varios roles, o hacia
 * usuarios específicos.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnviarNotificacionRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    // Opcional: si no se envía, el servicio asigna ANUNCIO_ADMIN o ANUNCIO_MODERADOR
    // según el rol de quien la envía.
    private NotificacionTipo tipo;

    @NotNull(message = "Debes indicar el alcance de la notificación")
    private AlcanceNotificacion alcance;

    // Usado solo cuando alcance = POR_ROL
    private List<Rol> roles;

    // Usado solo cuando alcance = USUARIOS_ESPECIFICOS
    private List<String> usernames;
}
