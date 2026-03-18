package com.photobogota.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para manejar operaciones con JWT (JSON Web Tokens)
 * 
 * Este servicio proporciona métodos para:
 * - Generar tokens JWT para usuarios autenticados
 * - Generar tokens de refresh para renovación de sesión
 * - Validar y extraer información de los tokens
 * - Verificar si un token ha expirado
 */
@Service
public class JwtService {

    /**
     * Clave secreta para firmar los tokens JWT
     * Se inyecta desde application.properties
     */
    @Value("${application.security.jwt.secret}")
    private String claveSecreta;

    /**
     * Tiempo de expiración del token JWT principal
     * Se inyecta desde application.properties (default: 24 horas = 86400000 ms)
     */
    @Value("${application.security.jwt.expiration}")
    private long tiempoExpiracion;

    /**
     * Tiempo de expiración del token de refresh
     * Se inyecta desde application.properties (default: 7 días = 604800000 ms)
     */
    @Value("${application.security.jwt.refresh-expiration}")
    private long tiempoExpiracionRefresh;

    /**
     * Extrae el nombre de usuario (subject) del token JWT
     * El subject es el identificador principal del usuario en el token
     * 
     * @param token el token JWT
     * @return el nombre de usuario contenido en el token
     */
    public String extraerNombreUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Extrae un claim específico del token JWT
     * Los claims son la información contenida en el token
     * 
     * @param token el token JWT
     * @param claimsResolver función para extraer el claim deseado
     * @return el valor del claim extraído
     */
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT con claims adicionales
     * Este método se usa para crear tokens de acceso con información extra
     * 
     * @param extraClaims claims adicionales a incluir en el token
     * @param nombreUsuario el nombre de usuario para el subject del token
     * @return el token JWT generado
     */
    public String generarToken(Map<String, Object> extraClaims, String nombreUsuario) {
        return construirToken(extraClaims, nombreUsuario, tiempoExpiracion);
    }

    /**
     * Genera un token JWT simple sin claims adicionales
     * Este es el método más común para generar tokens de acceso
     * 
     * @param nombreUsuario el nombre de usuario para el subject del token
     * @return el token JWT generado
     */
    public String generarToken(String nombreUsuario) {
        return construirToken(Map.of(), nombreUsuario, tiempoExpiracion);
    }

    /**
     * Genera un token de refresh para renovación de sesión
     * El token de refresh tiene una expiración más larga que el token de acceso
     * y se usa para obtener un nuevo token sin necesidad de volver a autenticar
     * 
     * @param nombreUsuario el nombre de usuario para el subject del token
     * @return el token de refresh generado
     */
    public String generarTokenRefresh(String nombreUsuario) {
        return construirToken(Map.of(), nombreUsuario, tiempoExpiracionRefresh);
    }

    /**
     * Construye el token JWT con los parámetros especificados
     * Este método privado es usado internamente por los métodos de generación
     * 
     * @param extraClaims claims adicionales
     * @param subject el subject (generalmente el nombre de usuario)
     * @param expiracion tiempo de expiración en milisegundos
     * @return el token JWT construido
     */
    private String construirToken(Map<String, Object> extraClaims, String subject, long expiracion) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiracion))
                .signWith(claveFirmado())
                .compact();
    }

    /**
     * Valida si el token es válido para el usuario especificado
     * Compara el nombre de usuario en el token con el esperado
     * y verifica que el token no haya expirado
     * 
     * @param token el token JWT a validar
     * @param nombreUsuario el nombre de usuario esperado
     * @return true si el token es válido, false en caso contrario
     */
    public boolean esTokenValido(String token, String nombreUsuario) {
        final String nombreUsuarioToken = extraerNombreUsuario(token);
        return (nombreUsuarioToken.equals(nombreUsuario)) && !estaTokenExpirado(token);
    }

    /**
     * Verifica si el token ha expirado
     * Compara la fecha de expiración del token con la fecha actual
     * 
     * @param token el token a verificar
     * @return true si el token está expirado, false en caso contrario
     */
    private boolean estaTokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token
     * 
     * @param token el token JWT
     * @return la fecha de expiración del token
     */
    private Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae todos los claims del token
     * Este método es usado internamente para obtener toda la información del token
     * 
     * @param token el token JWT
     * @return todos los claims del token
     */
    private Claims extraerTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(claveFirmado())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Obtiene la clave de firmado para firmar/verificar tokens
     * La clave debe estar codificada en Base64 y tener al menos 256 bits
     * 
     * @return la clave HMAC-SHA para firmado
     */
    private SecretKey claveFirmado() {
        byte[] bytesClave = Decoders.BASE64.decode(claveSecreta);
        return Keys.hmacShaKeyFor(bytesClave);
    }
}
