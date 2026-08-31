package com.photobogota.api.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.TipoSancion;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    @Test
    void credencialesInvalidas_devuelve401ConElMensaje() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Usuario o contraseña incorrectos");
        MockHttpServletRequest req = request("/api/v1/auth/login");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleInvalidCredentials(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().get("message")).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(respuesta.getBody().get("path")).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void noAutorizado_devuelve401ConElMensaje() {
        UnauthorizedException ex = new UnauthorizedException("Token no proporcionado");
        MockHttpServletRequest req = request("/api/v1/mi-perfil");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleUnauthorized(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().get("message")).isEqualTo("Token no proporcionado");
    }

    @Test
    void operacionInvalida_devuelve400ConElMensaje() {
        OperacionInvalidaException ex = new OperacionInvalidaException("No se puede procesar");
        MockHttpServletRequest req = request("/api/v1/aspirantes");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleOperacionInvalida(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("message")).isEqualTo("No se puede procesar");
    }

    @Test
    void cambioDeContrasena_devuelve400ConElMensaje() {
        CambioContrasenaException ex = new CambioContrasenaException("La contraseña actual no coincide");
        MockHttpServletRequest req = request("/api/v1/usuario/cambiar-contrasena");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleCambioContrasena(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("message")).isEqualTo("La contraseña actual no coincide");
    }

    @Test
    void recursoNoEncontrado_devuelve404ConElMensaje() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Spot no encontrado");
        MockHttpServletRequest req = request("/api/v1/spots/1");

        ResponseEntity<?> respuesta = handler.handleNotFound(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recursoYaExistente_devuelve409ConElMensaje() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("email", "x@example.com");
        MockHttpServletRequest req = request("/api/v1/auth/register");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleConflicts(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().get("message")).isEqualTo(ex.getMessage());
    }

    @Test
    void duplicadoDeSesion_devuelve409ConMensajePersonalizado() {
        DuplicateKeyException ex = new DuplicateKeyException(
                "E11000 duplicate key error collection: refresh_tokens");
        MockHttpServletRequest req = request("/api/v1/auth/login");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleConflicts(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().get("message"))
                .isEqualTo("Ya existe una sesión activa procesándose para este usuario");
    }

    @Test
    void duplicadoDeOtraColeccion_devuelve409ConMensajeGenerico() {
        DuplicateKeyException ex = new DuplicateKeyException(
                "E11000 duplicate key error collection: usuarios index: email_1 dup key");
        MockHttpServletRequest req = request("/api/v1/auth/register");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleConflicts(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().get("message"))
                .isEqualTo("El correo o nombre de usuario ya existe en PhotoBogota");
    }

    @Test
    void accesoDenegado_devuelve403ConMensajeGenerico() {
        AccessDeniedException ex = new AccessDeniedException("no");
        MockHttpServletRequest req = request("/api/v1/admin/usuarios");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleAccessDenied(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().get("message"))
                .isEqualTo("No tienes permiso para acceder a este recurso");
    }

    @Test
    void accesoProhibidoPorRegla_devuelve403ConElMensaje() {
        AccessForbiddenException ex = new AccessForbiddenException("No puedes reportar tu propia reseña");
        MockHttpServletRequest req = request("/api/v1/reportes");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleAccessForbidden(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().get("message")).isEqualTo("No puedes reportar tu propia reseña");
    }

    @Test
    void contenidoInapropiado_conPalabrasDevuelve400ConDetalles() {
        ContenidoInapropiadoException ex = new ContenidoInapropiadoException(
                "Contenido inapropiado", List.of("groseria"), TipoSancion.NOTIFICACION,
                java.time.LocalDateTime.now().plusDays(3), 1);
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleContenidoInapropiado(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("tipo", "NOTIFICACION")
                .containsEntry("contadorInfracciones", 1);
        assertThat(respuesta.getBody().get("palabrasDetectadas")).asList()
                .containsExactly("groseria");
        assertThat(respuesta.getBody()).containsKey("fechaExpiracion");
    }

    @Test
    void contenidoInapropiado_sinPalabras_devuelve403() {
        ContenidoInapropiadoException ex = new ContenidoInapropiadoException(
                "Estás sancionado", null, TipoSancion.BAN, null, 4);
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleContenidoInapropiado(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).containsEntry("tipo", "BAN")
                .containsEntry("contadorInfracciones", 4);
    }

    @Test
    void tokenExpirado_devuelve401() {
        ExpiredJwtException ex = mock(ExpiredJwtException.class);
        when(ex.getMessage()).thenReturn("token expirado");
        MockHttpServletRequest req = request("/api/v1/auth/refresh");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleExpiredJwt(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().get("message")).isEqualTo(
                "El token de autenticación ha expirado. Por favor inicie sesión nuevamente.");
    }

    @Test
    void tokenJwtInvalido_devuelve401() {
        SignatureException ex = mock(SignatureException.class);
        when(ex.getMessage()).thenReturn("firma inválida");
        MockHttpServletRequest req = request("/api/v1/auth/refresh");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleInvalidJwt(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().get("message")).isEqualTo("Token de autenticación inválido");
    }

    @Test
    void errorInesperado_devuelve500() {
        RuntimeException ex = new RuntimeException("boom");
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleGlobalException(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(respuesta.getBody().get("message"))
                .isEqualTo("Ocurrió un error inesperado. Por favor contacte al administrador.");
    }

    @Test
    void metodoNoSoportado_devuelve405() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleMethodNotSupported(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getBody().get("message").toString()).contains("El método DELETE no está soportado");
    }

    @Test
    void solicitudIncorrecta_devuelve400ConElMensaje() {
        org.apache.coyote.BadRequestException ex = new org.apache.coyote.BadRequestException("Petición mal formada");
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleBadRequest(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("message")).isEqualTo("Petición mal formada");
    }

    @Test
    void parametroFaltante_devuelve400MencionandoElParametro() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("categoria", "String");
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleMissingParams(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("message").toString()).contains("categoria");
    }

    @Test
    void tipoDeContenidoNoSoportado_devuelve415() {
        List<MediaType> tiposSoportados = java.util.Collections.singletonList(MediaType.APPLICATION_JSON);
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("Tipo de contenido no soportado", tiposSoportados);
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleMediaTypeNotSupported(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void jsonInvalido_devuelve400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON mal formado", mock(HttpInputMessage.class));
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleMessageNotReadable(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("message").toString())
                .contains("El cuerpo de la solicitud es inválido");
    }

    @Test
    void violacionDeRestricciones_devuelve400() {
        ConstraintViolationException ex = new ConstraintViolationException(Collections.emptySet());
        MockHttpServletRequest req = request("/api/v1/spots");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleConstraintViolation(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) respuesta.getBody().get("errors")).isEmpty();
    }

    @Test
    void erroresDeValidacion_devuelven400ConLosErroresDeCampo() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Miembro(), "miembro");
        bindingResult.addError(new FieldError("miembro", "email", "El email es requerido"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getMethod("metodoValidable", Miembro.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        MockHttpServletRequest req = request("/api/v1/auth/register");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidationErrors(ex, req);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) respuesta.getBody().get("errors"))
                .containsEntry("email", "El email es requerido");
    }

    public void metodoValidable(Miembro miembro) {
        // solo para construir el MethodParameter del test
    }
}