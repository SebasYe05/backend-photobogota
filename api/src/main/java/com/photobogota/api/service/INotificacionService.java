package com.photobogota.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.model.Spot;

public interface INotificacionService {

    // ---- Consulta / gestión de la bandeja del propio usuario ----

    Page<NotificacionResponseDTO> listarMisNotificaciones(String username, Pageable pageable, Boolean soloNoLeidas);

    long contarNoLeidas(String username);

    void marcarLeida(String id, String username);

    void marcarTodasLeidas(String username);

    void eliminarNotificacion(String id, String username);

    // ---- Preferencias ----

    PreferenciasNotificacionDTO obtenerPreferencias(String username);

    PreferenciasNotificacionDTO actualizarPreferencias(String username, PreferenciasNotificacionDTO dto);

    // ---- Envío manual (Admin / Moderador) ----

    void enviarNotificacionManual(EnviarNotificacionRequestDTO request, String emisorUsername, String emisorRol);

    // ---- Disparadores automáticos del sistema (llamados desde otros servicios) ----

    void notificarNuevoSpot(Spot spot);

    void notificarNuevaResena(Spot spot, Spot.Resena resena, String usuarioQueResenio);
}
