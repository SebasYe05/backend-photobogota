package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.PalabraProhibidaDTO;
import com.photobogota.api.dto.RegistroModeracionDTO;
import com.photobogota.api.dto.ResolverApelacionRequestDTO;
import com.photobogota.api.dto.SancionDTO;
import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.TipoContenidoModerado;

public interface IFiltroContenidoService {

    // ---- Filtro automático (detección y sanciones) ----

    /**
     * Verifica que el usuario pueda publicar contenido. Lanza
     * ContenidoInapropiadoException si tiene una sanción activa que bloquea
     * la publicación (MUTE, SUSPENSION o BAN).
     */
    void verificarPermisoPublicar(String nombreUsuario);

    /**
     * Analiza el contenido del usuario contra la lista de palabras prohibidas.
     * Si detecta una infracción, aplica la sanción progresiva correspondiente,
     * registra la acción en el historial de moderación y lanza
     * ContenidoInapropiadoException (el contenido no se publica).
     */
    void validarContenido(String nombreUsuario, TipoContenidoModerado tipo, String contenido);

    /**
     * Consulta la sanción actualmente activa del usuario (o null si no tiene).
     */
    SancionDTO obtenerSancionActual(String nombreUsuario);

    /**
     * Permite a un usuario baneado enviar una apelación de su ban indefinido.
     */
    void apelarBan(String nombreUsuario, String motivo);

    // ---- Gestión de la lista configurable de palabras prohibidas (ADMIN) ----

    List<PalabraProhibidaDTO> listarPalabras();

    PalabraProhibidaDTO crearPalabra(PalabraProhibidaDTO dto, String adminUsername);

    PalabraProhibidaDTO actualizarPalabra(String id, PalabraProhibidaDTO dto);

    void eliminarPalabra(String id);

    PalabraProhibidaDTO togglePalabra(String id);

    // ---- Historial de moderación y apelaciones (ADMIN) ----

    Page<RegistroModeracionDTO> listarHistorial(AccionModeracion accion, String nombreUsuario,
            TipoContenidoModerado tipoContenido, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    List<RegistroModeracionDTO> listarApelacionesPendientes();

    RegistroModeracionDTO resolverApelacion(String registroId, ResolverApelacionRequestDTO request,
            String adminUsername);
}
