package com.photobogota.api.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // MAP PARA ALMACENAR LOS BUCKETS DE CADA USUARIO/IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Resuelve o crea un Bucket de Bucket4j para gestionar la tasa de peticiones
     * (Rate Limit).
     * * El algoritmo identifica al cliente combinando su dirección IP con el tipo
     * de ruta,
     * lo que permite aplicar diferentes políticas de seguridad según la criticidad
     * del endpoint:
     * <ul>
     * <li><strong>Rutas de Auth Sensible</strong> (login, register, recovery): 10
     * peticiones por minuto.</li>
     * <li><strong>Rutas Generales</strong>: 60 peticiones por minuto.</li>
     * </ul>
     *
     * @param ip  Dirección IP del cliente que realiza la petición.
     * @param uri URI o endpoint al que se está intentando acceder.
     * @return El {@link Bucket} correspondiente a la clave generada (IP + Tipo de
     *         ruta).
     */
    public Bucket resolveBucket(String ip, String uri) {
        // Clasifica la ruta (ej. "auth" para endpoints sensibles o "general" para el
        // resto)
        String tipo = clasificarRuta(uri);

        // Genera una clave única que diferencia los límites por cliente y tipo de ruta
        // (ej. "192.168.1.100:auth")
        String key = ip + ":" + tipo;

        // Retorna el bucket asociado si ya existe, de lo contrario lo crea y lo
        // almacena de forma segura (Thread-safe)
        return buckets.computeIfAbsent(key, k -> crearBucket(tipo));
    }

    /**
     * Intenta procesar una petición verificando si el cliente tiene tokens
     * disponibles en su bucket.
     *
     * @param ip  Dirección IP del cliente.
     * @param uri URI o endpoint que el cliente está intentando consumir.
     * @return {@code true} si se permite la petición (se consume 1 token),
     *         {@code false} si se superó el límite (Rate Limit alcanzado).
     */
    public boolean tryConsume(String ip, String uri) {
        return resolveBucket(ip, uri).tryConsume(1);
    }

    /**
     * Clasifica la ruta solicitada para definir a qué grupo de seguridad pertenece.
     *
     * @param uri La ruta de la petición entrante.
     * @return {@code "auth_sensible"} para rutas críticas (seguridad o ataques), o
     *         {@code "general"} para el resto.
     */
    private String clasificarRuta(String uri) {
        if (uri.contains("/auth/login")
                || uri.contains("/auth/register")
                || uri.contains("/auth/refresh")
                || uri.contains("/passwords/")) {
            return "auth_sensible";
        }
        return "general";
    }

    /**
     * Construye un bucket de Bucket4j con la tasa y capacidad adecuadas según el
     * tipo de ruta,
     * utilizando la nueva sintaxis de configuración basada en el patrón Builder.
     *
     * @param tipo El tipo de ruta clasificada ("auth_sensible" o "general").
     * @return Un nuevo {@link Bucket} configurado con sus límites.
     */
    private Bucket crearBucket(String tipo) {
        Bandwidth limit = switch (tipo) {
            case "auth_sensible" ->
                // Límite estricto: máximo 10 peticiones por minuto (protección contra fuerza
                // bruta).
                Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build();
            default ->
                // Límite estándar: máximo 60 peticiones por minuto para endpoints de uso común.
                Bandwidth.builder()
                        .capacity(60)
                        .refillGreedy(60, Duration.ofMinutes(1))
                        .build();
        };

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIPs", buckets.size());

        List<Map<String, Object>> detalle = buckets.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("clave", entry.getKey());
                    info.put("tokensRestantes", entry.getValue().getAvailableTokens());
                    return info;
                })
                .toList();

        stats.put("buckets", detalle);
        return stats;
    }

}
