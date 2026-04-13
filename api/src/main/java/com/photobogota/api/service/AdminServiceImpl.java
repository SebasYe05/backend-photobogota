package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements IAdminService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioFactory usuarioFactory;

    @Override
    @Transactional
    public RegistroResponseDTO crearUsuario(CrearUsuarioRequestDTO dto) {
        log.info("Intentando registrar usuario con email: {}", dto.getEmail());

        // 1. Validaciones previas
        if (usuarioAuthRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        if (usuarioAuthRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new UsernameAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        // 2. Determinar el rol (por defecto MIEMBRO si no viene)
        Rol rol;
        try {
            rol = dto.getRol() != null ? Rol.valueOf(dto.getRol().toUpperCase()) : Rol.MIEMBRO;
        } catch (IllegalArgumentException e) {
            rol = Rol.MIEMBRO;
        }

        // 3. Generar ID compartido
        ObjectId id = new ObjectId();

        // 4. Crear Usuario (perfil) segun el rol
        Usuario usuario = usuarioFactory.crearUsuario(id, dto, rol);

        // 5. Crear UsuarioAuth (credenciales)
        UsuarioAuth usuarioAuth = UsuarioAuth.builder()
                .id(id)
                .nombreUsuario(dto.getNombreUsuario())
                .email(dto.getEmail())
                .rol(rol)
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .build();

        // 6. Persistencia
        try {
            usuarioRepository.save(usuario);
            usuarioAuthRepository.save(usuarioAuth);
        } catch (Exception e) {
            log.error("Error crítico al guardar el usuario: {}", e.getMessage());
            throw new RuntimeException("Error al registrar el usuario", e);
        }

        log.info("Usuario registrado exitosamente con ID: {} y rol: {}", id, rol);

        return RegistroResponseDTO.builder()
                .fechaRegistro(LocalDateTime.now())
                .mensaje("Usuario registrado exitosamente")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioListResponseDTO> listarUsuarios(Pageable pageable) {
        log.info("Listando todos los usuarios - página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Obtener todos los usuarios de la colección principal (perfiles)
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);

        // Combinar con los datos de autenticación
        List<UsuarioListResponseDTO> usuariosDTO = usuariosPage.getContent().stream()
                .map(usuario -> {
                    UsuarioAuth auth = usuarioAuthRepository.findById(usuario.getId())
                            .orElse(null);

                    if (auth == null) {
                        log.warn("UsuarioAuth no encontrado para ID: {}", usuario.getId());
                        return null;
                    }

                    return UsuarioListResponseDTO.builder()
                            .id(usuario.getId().toHexString())
                            .nombresCompletos(usuario.getNombresCompletos())
                            .nombreUsuario(auth.getNombreUsuario())
                            .email(auth.getEmail())
                            .rol(auth.getRol() != null ? auth.getRol().name() : "MIEMBRO")
                            .estadoCuenta(usuario.getEstadoCuenta())
                            .fechaRegistro(usuario.getFechaRegistro())
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return new PageImpl<>(usuariosDTO, pageable, usuariosPage.getTotalElements());
    }

    @Override
    @Transactional
    public void actualizarEstadoUsuario(String usuarioId, boolean activo) {
        log.info("Actualizando estado del usuario ID: {} a activo: {}", usuarioId, activo);

        ObjectId id = new ObjectId(usuarioId);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        usuario.setEstadoCuenta(activo);
        usuarioRepository.save(usuario);

        log.info("Estado actualizado correctamente");
    }

    @Override
    @Transactional
    public void eliminarUsuario(String usuarioId) {
        log.info("Eliminando usuario ID: {}", usuarioId);

        ObjectId id = new ObjectId(usuarioId);

        usuarioRepository.deleteById(id);
        usuarioAuthRepository.deleteById(id);

        log.info("Usuario eliminado correctamente");
    }
}
