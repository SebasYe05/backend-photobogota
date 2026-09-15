package com.photobogota.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import jakarta.servlet.ServletContext;

class WebMvcConfigTest {

    @TempDir
    Path tempDir;

    static class RegistroAbierto extends ResourceHandlerRegistry {
        RegistroAbierto(ApplicationContext context, ServletContext servletContext) {
            super(context, servletContext);
        }
    }

    private WebMvcConfig configCon(String uploadDir) {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(config, "uploadDir", uploadDir);
        return config;
    }

    @SuppressWarnings("unchecked")
    private List<String> ubicacionesRegistradas(ResourceHandlerRegistry registry) throws Exception {
        Field registrations = ResourceHandlerRegistry.class.getDeclaredField("registrations");
        registrations.setAccessible(true);
        List<ResourceHandlerRegistration> lista =
                (List<ResourceHandlerRegistration>) registrations.get(registry);

        ResourceHandlerRegistration uploads = lista.stream()
                .filter(r -> {
                    String[] patterns = (String[]) ReflectionTestUtils.getField(r, "pathPatterns");
                    return patterns != null && patterns.length == 1
                            && "/uploads/**".equals(patterns[0]);
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se registraron handlers de /uploads/**"));

        return (List<String>) ReflectionTestUtils.getField(uploads, "locationValues");
    }

    private ResourceHandlerRegistry registroCon(ApplicationContext context) {
        return new RegistroAbierto(context, new MockServletContext());
    }

    @Test
    void registraElHandlerDeUploadsEnElDirectorioAbsolutoConfigurado() throws Exception {
        ApplicationContext context = new AnnotationConfigWebApplicationContext();
        ResourceHandlerRegistry registry = registroCon(context);

        configCon(tempDir.toString()).addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isTrue();
        assertThat(ubicacionesRegistradas(registry))
                .containsExactly("file:" + tempDir.toAbsolutePath() + "/");
    }

    @Test
    void lasRutasRelativasSeResuelvenContraElDirectorioDeTrabajo() throws Exception {
        ApplicationContext context = new AnnotationConfigWebApplicationContext();
        ResourceHandlerRegistry registry = registroCon(context);

        configCon("uploads").addResourceHandlers(registry);

        Path esperado = Paths.get("uploads").toAbsolutePath();
        assertThat(ubicacionesRegistradas(registry)).containsExactly("file:" + esperado + "/");
    }
}