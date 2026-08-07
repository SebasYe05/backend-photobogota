package com.photobogota.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.Rol;
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

    void notificarNuevaCalificacion(Spot spot, Calificacion calificacion, String usuarioQueCalifico);

    // Notificación de sistema puntual para un usuario ya identificado por su
    // nombreUsuario (ej: aprobación de membresía, envío de credenciales).
    // A diferencia de notificarNuevoSpot/notificarNuevaResena, esta no
    // respeta preferencias de silencio: es información crítica de la cuenta,
    // no un aviso discrecional.
    void notificarSistema(String destinatarioUsername, String titulo, String mensaje);

    // Notifica a TODOS los usuarios que tengan un rol dado (ej: avisar a
    // todo el equipo de MOD que hay un reporte nuevo en su cola, o a todo
    // ADMIN que se les escaló un caso). Igual que notificarSistema, es
    // información operativa del staff y no respeta el silencio de
    // preferencias: quien tiene ese rol necesita enterarse igual.
    void notificarPorRol(Rol rol, String titulo, String mensaje, String emisorUsername);
}
