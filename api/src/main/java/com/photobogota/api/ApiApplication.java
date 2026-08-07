package com.photobogota.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
// Habilita los métodos anotados con @Scheduled en toda la app. Sin esto,
// los @Scheduled existentes (MantenimientoScheduler, la limpieza de cuentas
// eliminadas en EliminacionCuentaServiceImpl) y el nuevo
// EscalamientoReportesScheduler (escalamiento automático de reportes de
// socio vencidos, HU 15 pt 7) quedan escritos pero nunca se ejecutan.
@EnableScheduling
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}