package com.photobogota.api.service;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.mapper.UsuarioMapper;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional // Asegura que se guarden AMBOS o NINGUNO
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
                .fechaRegistro(LocalDateTime.now())
                .mensaje("Usuario registrado exitosamente")
                .build();
    }
}
