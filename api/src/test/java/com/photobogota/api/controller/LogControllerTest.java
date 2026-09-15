package com.photobogota.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LogControllerTest extends ControllerTestSupport {

    private final LogController controller = new LogController();

    @AfterEach
    void limpiarLogs() throws Exception {
        Path base = Path.of("target", "test-logs");
        if (Files.exists(base)) {
            try (var stream = Files.walk(base)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void getLogs_cuandoNoExisteElArchivo_devuelve200ConMensaje() throws Exception {
        ReflectionTestUtils.setField(controller, "logPathDir", "target/test-logs/vacio");

        mvc(controller)
                .perform(get("/api/v1/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("El archivo de log aún no ha sido creado."));
    }

    @Test
    void getLogs_devuelveUltimasLineasInvertidas() throws Exception {
        Path dir = Path.of("target", "test-logs", "contenido");
        Files.createDirectories(dir);
        Files.write(dir.resolve("photobogota.log"),
                java.util.List.of("linea-1", "linea-2", "linea-3"));
        ReflectionTestUtils.setField(controller, "logPathDir", dir.toString());

        mvc(controller)
                .perform(get("/api/v1/admin/logs").param("lines", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("linea-3"))
                .andExpect(jsonPath("$[1]").value("linea-2"));
    }

    @Test
    void getLogs_pidiendoSoloErroresUsaElArchivoDeErrores() throws Exception {
        Path dir = Path.of("target", "test-logs", "errores");
        Files.createDirectories(dir);
        Files.write(dir.resolve("photobogota-error.log"), java.util.List.of("error-1"));
        ReflectionTestUtils.setField(controller, "logPathDir", dir.toString());

        mvc(controller)
                .perform(get("/api/v1/admin/logs").param("errorsOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("error-1"));
    }

    @Test
    void getLogFiles_cuandoLaRutaNoExiste_devuelve200Vacio() throws Exception {
        ReflectionTestUtils.setField(controller, "logPathDir", "target/test-logs/inexistente");

        mvc(controller)
                .perform(get("/api/v1/admin/logs/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getLogFiles_devuelveLosArchivosDeLaCarpeta() throws Exception {
        Path dir = Path.of("target", "test-logs", "archivos");
        Files.createDirectories(dir);
        Files.write(dir.resolve("photobogota.log"), "contenido".getBytes());
        ReflectionTestUtils.setField(controller, "logPathDir", dir.toString());

        mvc(controller)
                .perform(get("/api/v1/admin/logs/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("photobogota.log"))
                .andExpect(jsonPath("$[0].size").value(9));
    }
}