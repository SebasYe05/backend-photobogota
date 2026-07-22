package com.photobogota.api.dto;

import java.util.List;

import com.photobogota.api.model.CanalNotificacion;
import com.photobogota.api.model.NotificacionTipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Se usa tanto para devolver las preferencias actuales del usuario (GET)
 * como para actualizarlas parcialmente (PUT). En la actualización, cualquier
 * campo que llegue en null se ignora y conserva el valor previamente guardado.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreferenciasNotificacionDTO {

    private Boolean notificacionesActivas;
    private CanalNotificacion canalPreferido;
    private List<NotificacionTipo> tiposSilenciados;
    private List<String> localidadesInteres;
    private List<String> categoriasInteres;
}
