package com.photobogota.api.controller;

import com.photobogota.api.dto.LogFileInfo;
import com.photobogota.api.utils.StreamUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    @Value("${logging.file.path:logs}")
    private String logPathDir;

    private static final String LOG_FILE = "photobogota.log";
    private static final String ERROR_LOG_FILE = "photobogota-error.log";

    @GetMapping
    public ResponseEntity<List<String>> getLogs(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "false") boolean errorsOnly,
            HttpServletRequest request) {

        log.info("Consulta de logs desde: {}", request.getRemoteAddr());

        String fileName = errorsOnly ? ERROR_LOG_FILE : LOG_FILE;
        Path path = Paths.get(logPathDir, fileName);

        if (!Files.exists(path)) {
            return ResponseEntity.ok(List.of("El archivo de log aún no ha sido creado."));
        }

        // El try-with-resources cierra el archivo automáticamente al terminar
        try (Stream<String> stream = Files.lines(path)) {
            List<String> lastLines = stream.collect(StreamUtils.<String>lastN(lines));
            Collections.reverse(lastLines); // El log más nuevo aparecerá primero en el JSON
            return ResponseEntity.ok(lastLines);
        } catch (IOException e) {
            log.error("Error leyendo logs: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<LogFileInfo>> getLogFiles() {

        Path root = Paths.get(logPathDir);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.error("La ruta de logs no es válida: {}", logPathDir);
            return ResponseEntity.ok(Collections.emptyList());
        }

        try (Stream<Path> paths = Files.list(Paths.get(logPathDir))) {
            List<LogFileInfo> files = paths
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        try {
                            return new LogFileInfo(p.getFileName().toString(),
                                    Files.size(p),
                                    Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            log.warn("No se pudo leer el archivo: {}", p.getFileName());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            log.error("Error listando archivos de log: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}