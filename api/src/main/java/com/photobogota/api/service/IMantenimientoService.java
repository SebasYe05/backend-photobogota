package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.dto.MantenimientoResponseDTO;
import com.photobogota.api.dto.ProgramarMantenimientoRequestDTO;

public interface IMantenimientoService {

    // Programa la ventana y dispara el aviso a todos los usuarios (vía INotificacionService)
    MantenimientoResponseDTO programar(ProgramarMantenimientoRequestDTO request, String adminUsername);

    // Cancela una ventana ya programada y avisa a todos que ya no aplica
    void cancelar(String id, String adminUsername);

    // Consultado por el front (banner) y por el MantenimientoFilter en cada petición
    EstadoMantenimientoDTO obtenerEstado();

    // Listado para la pantalla de Admin (programados/activos, sin cancelar)
    List<MantenimientoResponseDTO> listarProgramados();

    // Llamado cada minuto por MantenimientoScheduler: detecta ventanas que
    // acaban de iniciar/terminar y envía el aviso de "mantenimiento finalizado".
    void revisarYNotificarCambiosDeEstado();
}
