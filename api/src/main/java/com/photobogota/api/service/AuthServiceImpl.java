package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.config.JwtService;
import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.LogoutResponseDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.SolicitarRecuperacionDTO;
import com.photobogota.api.dto.UsuarioResumenDTO;
import com.photobogota.api.dto.VerificarCodigoDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.InvalidCredentialsException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.mapper.UsuarioMapper;
import com.photobogota.api.model.CodigoRecuperacion;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.repository.CodigoRecuperacionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de autenticación.
 * Maneja el login de usuarios generando tokens JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final JwtService jwtService;
    private final IRefreshToken refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final CodigoRecuperacionRepository codigoRecuperacionRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public RegistroResponseDTO registrar(RegistroRequestDTO dto) {
        log.info("Intentando registrar usuario con email: {}", dto.getEmail());

        // Verificar si el email ya está registrado en usuarios-auth
        if (usuarioAuthRepository.existsByEmail(dto.getEmail())) {
            log.warn("Intento de registro con email existente: {}", dto.getEmail());
            throw new EmailAlreadyExistsException(
                    "El email " + dto.getEmail() + " ya está registrado en el sistema");
        }

        if (usuarioAuthRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            log.warn("Intento de registro con nombre de usuario existente: {}", dto.getNombreUsuario());
            throw new UsernameAlreadyExistsException(
                    "El nombre de usuario " + dto.getNombreUsuario() + " ya está en uso");
        }

        // Generar el ID que se usará en ambas colecciones
        ObjectId id = new ObjectId();

        // Crear el perfil del usuario (colección: usuarios)
        Miembro miembro = usuarioMapper.toMiembroEntity(dto);
        miembro.setId(id);

        // Inicializar puntos y nivel para nuevos miembros
        miembro.setPuntos(0L);
        miembro.setNivel(1);

        // Crear las credenciales (colección: usuarios-auth) con el mismo ID
        UsuarioAuth usuarioAuth = UsuarioAuth.builder()
                .id(id)
                .nombreUsuario(dto.getNombreUsuario())
                .email(dto.getEmail())
                .rol(Rol.MIEMBRO)
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .build();

        // Guardar en ambas colecciones con el mismo ID
        try {
            usuarioRepository.save(miembro);
            usuarioAuthRepository.save(usuarioAuth);
        } catch (Exception e) {
            log.error("Error crítico al guardar el usuario: {}", e.getMessage());
            throw new RuntimeException("Error al registrar el usuario", e);
        }

        log.info("Usuario registrado exitosamente con ID: {}", id.toString());

        // Retornar solo la fecha de registro
        return RegistroResponseDTO.builder()
                .mensaje("Usuario registrado exitosamente")
                .build();
    }

    /**
     * Autentica a un usuario con sus credenciales.
     * Busca el usuario por email o nombre de usuario, verifica la contraseña y
     * genera los tokens.
     * 
     * @param request DTO con las credenciales del usuario (login y contraseña)
     * @return LoginResponseDTO con el token JWT, refresh token e información del
     *         usuario
     * @throws InvalidCredentialsException si las credenciales son inválidas
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. Buscar al usuario por email O por nombre de usuario
        UsuarioAuth usuario = usuarioAuthRepository.findByEmailOrNombreUsuario(request.getLogin(), request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        // 2. Verificar si la contraseña coincide
        if (!passwordEncoder.matches(request.getContrasena(), usuario.getContrasena())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        // 3. Obtener el nivel del usuario desde la colección de usuarios
        // Solo los miembros tienen nivel
        Integer nivel = null;
        if (usuario.getId() != null && usuario.getRol() == Rol.MIEMBRO) {
            Optional<?> usuarioOpt = usuarioRepository.findById(usuario.getId());
            if (usuarioOpt.isPresent() && usuarioOpt.get() instanceof Miembro) {
                nivel = ((Miembro) usuarioOpt.get()).getNivel();
            }
        }

        // 4. Generar el token JWT con claims adicionales (rol y email)
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", usuario.getRol().name());
        extraClaims.put("email", usuario.getEmail());

        String token = jwtService.generarToken(extraClaims, usuario.getNombreUsuario());

        // 5. Generar el refresh token
        String refreshToken = refreshTokenService.crearRefreshToken(usuario.getEmail());

        // 6. Mapear y retornar la respuesta
        return LoginResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(usuario.getRol().name())
                .nivel(nivel)
                .mensaje("Autenticación exitosa")
                .build();
    }

    /**
     * Refresca el token JWT usando un refresh token válido.
     * Valida el refresh token, genera un nuevo JWT y un nuevo refresh token.
     * 
     * @param refreshToken El token de refresh
     * @return LoginResponseDTO con el nuevo token JWT y refresh token
     * @throws RuntimeException si el refresh token es inválido o ha expirado
     */
    @Override
    public LoginResponseDTO refreshToken(String refreshToken) {
        // 1. Validar el refresh token y obtener el email
        String email = refreshTokenService.obtenerEmailSiValido(refreshToken);

        // 2. Buscar el usuario por email
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // 3. Obtener el nivel del usuario
        Integer nivel = null;
        if (usuario.getId() != null && usuario.getRol() == Rol.MIEMBRO) {
            Optional<?> usuarioOpt = usuarioRepository.findById(usuario.getId());
            if (usuarioOpt.isPresent() && usuarioOpt.get() instanceof Miembro) {
                nivel = ((Miembro) usuarioOpt.get()).getNivel();
            }
        }

        // 4. Generar el nuevo token JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", usuario.getRol().name());
        extraClaims.put("email", usuario.getEmail());

        String newToken = jwtService.generarToken(extraClaims, usuario.getNombreUsuario());

        // 5. Generar un nuevo refresh token (esto también invalida el anterior)
        String newRefreshToken = refreshTokenService.crearRefreshToken(usuario.getEmail());

        // 6. Retornar la respuesta con los nuevos tokens
        return LoginResponseDTO.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .email(usuario.getEmail())
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(usuario.getRol().name())
                .nivel(nivel)
                .mensaje("Token refrescado exitosamente")
                .build();
    }

    /**
     * Cierra la sesión del usuario revocando el refresh token.
     * Invalida el refresh token proporcionado para que no pueda ser usado
     * nuevamente.
     * 
     * @param refreshToken El token de refresh a revocar
     * @return DTO con el mensaje de confirmación
     */
    @Override
    public LogoutResponseDTO logout(String refreshToken) {
        log.info("Intentando cerrar sesión con refresh token");

        // Revocar el refresh token
        refreshTokenService.revocarToken(refreshToken);

        log.info("Sesión cerrada exitosamente");

        return LogoutResponseDTO.builder()
                .mensaje("Sesión cerrada exitosamente")
                .build();
    }

    /**
     * Obtiene un resumen de los datos del usuario autenticado.
     * Busca el usuario por nombre de usuario y retorna sus datos básicos.
     * 
     * @param nombreUsuario El nombre de usuario del usuario autenticado
     * @return DTO con los datos básicos del usuario (nombre, foto, rol)
     */
    @Override
    public UsuarioResumenDTO getResumenUsuario(String nombreUsuario) {
        log.info("Obteniendo resumen del usuario: {}", nombreUsuario);

        // Buscar el usuario por nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        // Obtener el perfil del usuario desde la colección de usuarios
        String nombresCompletos = null;
        String fotoPerfil = null;
        Integer nivel = null;

        if (usuarioAuth.getId() != null) {
            Optional<?> usuarioOpt = usuarioRepository.findById(usuarioAuth.getId());
            if (usuarioOpt.isPresent()) {
                Object userObj = usuarioOpt.get();
                if (userObj instanceof Usuario usuario) { // Pattern Matching de Java 16+
                    nombresCompletos = usuario.getNombresCompletos();
                    fotoPerfil = usuario.getFotoPefil();

                    // Si es un Miembro, extraemos el nivel 
                    if (userObj instanceof Miembro miembro) {
                        nivel = miembro.getNivel();
                    }
                }
            }
        }

        log.info("Resumen del usuario obtenido exitosamente");

        return UsuarioResumenDTO.builder()
                .nombreUsuario(usuarioAuth.getNombreUsuario())
                .nombresCompletos(nombresCompletos)
                .fotoPerfil(fotoPerfil)
                .rol(usuarioAuth.getRol().name())
                .nivel(nivel) 
                .build();
    }

    /**
     * Solicita un código de recuperación de contraseña.
     * Genera un código numérico de 6 dígitos, lo guarda en la base de datos
     * y envía por correo electrónico al usuario.
     * 
     * @param dto DTO con el email del usuario
     * @return Mensaje de confirmación
     */
    @Override
    public String solicitarRecuperacionContrasena(SolicitarRecuperacionDTO dto) {
        String email = dto.getEmail();
        log.info("Solicitando recuperación de contraseña para: {}", email);

        // Verificar que el usuario existe
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "No existe una cuenta con el email proporcionado"));

        // Eliminar códigos anteriores del mismo email
        codigoRecuperacionRepository.deleteByEmail(email);

        // Generar código numérico de 6 dígitos
        String codigo = String.format("%06d", new java.util.Random().nextInt(1000000));

        // Crear el registro del código con fecha de expiración (15 minutos)
        LocalDateTime ahora = LocalDateTime.now();
        CodigoRecuperacion codigoRecuperacion = CodigoRecuperacion.builder()
                .email(email)
                .codigo(codigo)
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plusMinutes(15))
                .usado(false)
                .build();

        // Guardar el código
        codigoRecuperacionRepository.save(codigoRecuperacion);

        // Construir el HTML y enviar el correo
        String htmlContent = emailService.construirHtmlRecuperacion(
                usuario.getNombreUsuario(), codigo);
        emailService.enviarCorreoHtml(email, "Recuperar Contraseña - PhotoBogota", htmlContent);

        log.info("Código de recuperación enviado exitosamente a: {}", email);
        return "Se ha enviado un código de verificación a tu correo electrónico";
    }

    /**
     * Verifica el código de recuperación y cambia la contraseña del usuario.
     * 
     * @param dto DTO con el email, código y nueva contraseña
     * @return Mensaje de confirmación
     */
    @Override
    public String verificarCodigoYCambiarContrasena(VerificarCodigoDTO dto) {
        String email = dto.getEmail();
        String codigoIngresado = dto.getCodigo();
        String nuevaContrasena = dto.getNuevaContrasena();

        log.info("Verificando código de recuperación para: {}", email);

        // Buscar el código de recuperación
        CodigoRecuperacion codigoRecuperacion = codigoRecuperacionRepository
                .findByEmailAndCodigo(email, codigoIngresado)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Código de verificación inválido"));

        // Verificar si el código ya fue usado
        if (codigoRecuperacion.isUsado()) {
            throw new InvalidCredentialsException(
                    "El código de verificación ya ha sido utilizado");
        }

        // Verificar si el código ha expirado
        if (codigoRecuperacion.estaExpirado()) {
            throw new InvalidCredentialsException(
                    "El código de verificación ha expirado. Por favor, solicita uno nuevo");
        }

        // Marcar el código como usado
        codigoRecuperacion.setUsado(true);
        codigoRecuperacionRepository.save(codigoRecuperacion);

        // Buscar el usuario y actualizar la contraseña
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Usuario no encontrado"));

        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioAuthRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para: {}", email);
        return "Contraseña actualizada exitosamente. Ya puedes iniciar sesión con tu nueva contraseña";
    }

}
