package com.photobogota.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    @ExceptionHandler({ResourceAlreadyExistsException.class, DuplicateKeyException.class})
    public ResponseEntity<Map<String, Object>> handleConflicts(
            Exception ex, HttpServletRequest request) {

        log.warn("Conflicto de datos detectado: {}", ex.getMessage());

        String message = ex instanceof DuplicateKeyException
                ? "El correo o nombre de usuario ya existe en PhotoBogota"
                : ex.getMessage();

        Map<String, Object> body = buildBody(HttpStatus.CONFLICT, message, request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // 3. Credenciales inválidas (401)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        log.warn("Credenciales inválidas en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
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
}