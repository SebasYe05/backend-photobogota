package com.photobogota.api.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.photobogota.api.service.IMantenimientoService;

import lombok.RequiredArgsConstructor;

/**
 * Revisa cada minuto si alguna ventana de mantenimiento programada acaba de
 * iniciar o de terminar, para mantener las banderas de aviso consistentes y
 * notificar automáticamente cuando el servicio vuelve a la normalidad.
 * Requiere @EnableScheduling en ApiApplication.
 */
@Component
@RequiredArgsConstructor
public class MantenimientoScheduler {

    private final IMantenimientoService mantenimientoService;

    @Scheduled(cron = "0 * * * * *")
    public void revisarVentanasDeMantenimiento() {
        mantenimientoService.revisarYNotificarCambiosDeEstado();
    }
}
