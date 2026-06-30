package com.photobogota.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.apache.coyote.BadRequestException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Errores de validación (campos vacíos, formatos incorrectos)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Error de validación en petición: {}", ex.getBindingResult().getObjectName());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, null, request);
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 2. Conflictos de unicidad (email, username, duplicados en DB)
    @ExceptionHandler({ ResourceAlreadyExistsException.class, DuplicateKeyException.class })
    public ResponseEntity<Map<String, Object>> handleConflicts(
            Exception ex, HttpServletRequest request) {

        log.warn("Conflicto de datos detectado: {}", ex.getMessage());

        String message = ex instanceof DuplicateKeyException
                ? "El correo o nombre de usuario ya existe en PhotoBogota"
                : ex.getMessage();

        Map<String, Object> body = buildBody(HttpStatus.CONFLICT, message, request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // 2.1 Método HTTP no soportado (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Método HTTP no soportado en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(
                HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está soportado para esta ruta. Métodos permitidos: "
                        + ex.getSupportedMethods(),
                request);

        return new ResponseEntity<>(body, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 3. Credenciales inválidas (401)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        log.warn("Credenciales inválidas en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // 3.1 Error en cambio de contraseña (400)
    @ExceptionHandler(CambioContrasenaException.class)
    public ResponseEntity<Map<String, Object>> handleCambioContrasena(
            CambioContrasenaException ex, HttpServletRequest request) {

        log.warn("Error en cambio de contraseña en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 4. Acceso denegado (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.error("Intento de acceso no autorizado a {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(
                HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este recurso", request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // 3.1 No autorizado personalizado (401)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {

        log.warn("No autorizado en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // 5. Cualquier otra excepción no manejada (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("CRITICAL ERROR en {}", request.getRequestURI(), ex);

        Map<String, Object> body = buildBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Por favor contacte al administrador.",
                request);
        body.put("error", "Internal Server Error");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- Utilidad para construir el body base ---
    private Map<String, Object> buildBody(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("path", request.getRequestURI());
        if (message != null) {
            body.put("message", message);
        }
        return body;
    }

    // 6. Recurso no encontrado (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return new ResponseEntity<>(buildBody(HttpStatus.NOT_FOUND, ex.getMessage(), req), HttpStatus.NOT_FOUND);
    }

    // 7. Solicitud con cuerpo JSON inválido (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("JSON inválido en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud es inválido o tiene un formato incorrecto", request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 8. Parámetros requeridos faltantes (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Parámetro requerido faltante en {}: {}", request.getRequestURI(), ex.getParameterName());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getParameterName() + "' es requerido", request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 9. Violación de restricciones de validación (400)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Violación de restricciones en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Error de validación", request);
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 10. Tipo de contenido no soportado (415)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Content-Type no soportado en {}: {}", request.getRequestURI(), ex.getContentType());

        Map<String, Object> body = buildBody(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El tipo de contenido " + ex.getContentType() + " no está soportado. Tipos permitidos: "
                        + ex.getSupportedMediaTypes(),
                request);
        return new ResponseEntity<>(body, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // 11. Solicitud incorrecta (400)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        log.warn("Solicitud incorrecta en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 12. Token JWT expirado (401)
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {

        log.warn("Token JWT expirado en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.UNAUTHORIZED,
                "El token de autenticación ha expirado. Por favor inicie sesión nuevamente.", request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // 12.1. Token JWT inválido (401)
    @ExceptionHandler({ MalformedJwtException.class, SignatureException.class })
    public ResponseEntity<Map<String, Object>> handleInvalidJwt(
            Exception ex, HttpServletRequest request) {

        log.warn("Token JWT inválido en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.UNAUTHORIZED,
                "Token de autenticación inválido", request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

}