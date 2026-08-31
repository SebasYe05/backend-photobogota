package com.photobogota.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

    private static final String URL_BASE = "http://localhost:8080/uploads";

    private LocalStorageService localStorageService;
    private Path rutaBase;

    @BeforeEach
    void prepararAlmacenamiento() throws IOException {
        rutaBase = Files.createTempDirectory("photobogota-test");
        localStorageService = new LocalStorageService();
        ReflectionTestUtils.setField(localStorageService, "rutaBase", rutaBase.toString());
        ReflectionTestUtils.setField(localStorageService, "urlBase", URL_BASE);
    }

    @AfterEach
    void limpiarAlmacenamiento() throws IOException {
        try (var walk = Files.walk(rutaBase)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort durante la limpieza del test
                }
            });
        }
    }

    private MultipartFile archivoDeEjemplo() {
        return new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[] { 1, 2, 3, 4 });
    }

    @Test
    void guardar_copiaElArchivoYDevuelveUnaUrl() throws IOException {
        String url = localStorageService.guardar(archivoDeEjemplo(), "spots");

        assertThat(url).startsWith(URL_BASE + "/spots/");
        assertThat(url).endsWith("foto.jpg");

        String nombreArchivo = Path.of(url.replace(URL_BASE + "/", "")).getFileName().toString();
        byte[] contenido = Files.readAllBytes(rutaBase.resolve("spots").resolve(nombreArchivo));
        assertThat(contenido).containsExactly(1, 2, 3, 4);
    }

    @Test
    void guardar_creaLosDirectoriosDeLaCarpeta() {
        String url = localStorageService.guardar(archivoDeEjemplo(), "perfil/avatars");

        assertThat(url).startsWith(URL_BASE + "/perfil/avatars/");
        assertThat(Files.isDirectory(rutaBase.resolve("perfil").resolve("avatars"))).isTrue();
    }

    @Test
    void guardar_generaNombresUnicosPorArchivo() {
        String primera = localStorageService.guardar(archivoDeEjemplo(), "spots");
        String segunda = localStorageService.guardar(archivoDeEjemplo(), "spots");

        assertThat(segunda).isNotEqualTo(primera);
        assertThat(Files.isRegularFile(rutaBase.resolve("spots"))).isFalse();
    }

    @Test
    void eliminar_borraElArchivoDelDisco() {
        String url = localStorageService.guardar(archivoDeEjemplo(), "spots");

        localStorageService.eliminar(url);

        String nombreArchivo = Path.of(url.replace(URL_BASE + "/", "")).getFileName().toString();
        assertThat(Files.exists(rutaBase.resolve("spots").resolve(nombreArchivo))).isFalse();
    }

    @Test
    void eliminar_conUrlInexistente_noLanzaExcepcion() {
        localStorageService.eliminar(URL_BASE + "/spots/no-existe.jpg");

        assertThat(Files.exists(rutaBase.resolve("spots").resolve("no-existe.jpg"))).isFalse();
    }
}