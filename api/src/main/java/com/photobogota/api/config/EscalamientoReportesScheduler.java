package com.photobogota.api.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.photobogota.api.service.ReporteServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Revisa periódicamente los reportes asignados a un SOCIO que llevan más de
 * 24h sin respuesta y los escala automáticamente a moderación (HU 15,
 * criterio de aceptación 7: "Escalamiento automático si no hay respuesta a
 * tiempo"). Requiere @EnableScheduling en ApiApplication.
 */
@Component
@RequiredArgsConstructor
public class EscalamientoReportesScheduler {

    private final ReporteServiceImpl reporteService;

    // Cada hora, en el minuto 0.
    @Scheduled(cron = "0 0 * * * *")
    public void escalarReportesDeSocioVencidos() {
        reporteService.escalarVencidosAutomaticamente();
    }
}
