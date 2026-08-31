package com.photobogota.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.service.IMantenimientoService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MantenimientoSchedulerTest {

    @Mock
    private IMantenimientoService mantenimientoService;

    @InjectMocks
    private MantenimientoScheduler mantenimientoScheduler;

    @Test
    void revisarVentanasDeMantenimiento_delegaEnElServicio() {
        mantenimientoScheduler.revisarVentanasDeMantenimiento();

        verify(mantenimientoService).revisarYNotificarCambiosDeEstado();
    }
}