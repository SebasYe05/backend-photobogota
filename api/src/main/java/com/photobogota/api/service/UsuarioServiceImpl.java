package com.photobogota.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.exception.CambioContrasenaException;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Usuario;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto) {
        log.info("Editando perfil del usuario: {}", nombreUsuario);

        // Buscar el usuario en la colección de auth por nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Buscar el perfil del usuario en la colección de usuarios
        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de usuario no encontrado"));

        // Actualizar los campos proporcionados
        if (dto.getNombresCompletos() != null) {
            usuario.setNombresCompletos(dto.getNombresCompletos());
        }
        if (dto.getTelefono() != null) {
            usuario.setTelefono(dto.getTelefono());
        }
        if (dto.getFotoPerfil() != null) {
            usuario.setFotoPefil(dto.getFotoPerfil());
        }
        if (dto.getBiografia() != null) {
            usuario.setBiografia(dto.getBiografia());
        }

        // Guardar los cambios
        usuarioRepository.save(usuario);

        log.info("Perfil actualizado exitosamente para: {}", nombreUsuario);

        // Retornar el perfil actualizado
        return construirPerfilDTO(usuario, usuarioAuth);
    }

    @Override
    public PerfilUsuarioDTO obtenerPerfil(String nombreUsuario) {
        log.info("Obteniendo perfil del usuario: {}", nombreUsuario);

        // Buscar el usuario en la colección de auth por nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Buscar el perfil del usuario en la colección de usuarios
        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de usuario no encontrado"));

        return construirPerfilDTO(usuario, usuarioAuth);
    }

    @Override
    public CambiarContrasenaResponseDTO cambiarContrasena(String nombreUsuario, CambiarContrasenaDTO dto) {
        log.info("Solicitando cambio de contraseña para el usuario: {}", nombreUsuario);

        // Validar que la nueva contraseña y la confirmación coincidan
        if (!dto.getNuevaContrasena().equals(dto.getConfirmarContrasena())) {
            throw new CambioContrasenaException("La nueva contraseña y la confirmación no coinciden");
        }

        // Buscar el usuario en la colección de auth por nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new CambioContrasenaException("Usuario no encontrado"));

        // Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(dto.getContrasenaActual(), usuarioAuth.getContrasena())) {
            throw new CambioContrasenaException("La contraseña actual es incorrecta");
        }

        // Verificar que la nueva contraseña sea diferente a la actual
        if (passwordEncoder.matches(dto.getNuevaContrasena(), usuarioAuth.getContrasena())) {
            throw new CambioContrasenaException("La nueva contraseña debe ser diferente a la actual");
        }

        // Actualizar la contraseña
        usuarioAuth.setContrasena(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuarioAuthRepository.save(usuarioAuth);

        log.info("Contraseña actualizada exitosamente para el usuario: {}", nombreUsuario);

        return CambiarContrasenaResponseDTO.builder()
                .mensaje("Contraseña actualizada exitosamente")
                .build();
    }

    /**
     * Método auxiliar para construir el PerfilUsuarioDTO
     */
    private PerfilUsuarioDTO construirPerfilDTO(Usuario usuario, UsuarioAuth usuarioAuth) {
        PerfilUsuarioDTO.PerfilUsuarioDTOBuilder builder = PerfilUsuarioDTO.builder()
                .id(usuario.getId())
                .nombresCompletos(usuario.getNombresCompletos())
                .email(usuarioAuth.getEmail())
                .nombreUsuario(usuarioAuth.getNombreUsuario())
                .fotoPefil(usuario.getFotoPefil())
                .biografia(usuario.getBiografia())
                .tipoUsuario(usuarioAuth.getRol().name());

        // Si es un Miembro, incluir puntos y nivel
        if (usuario instanceof Miembro) {
            Miembro miembro = (Miembro) usuario;
            builder.puntos(miembro.getPuntos())
                    .nivel(miembro.getNivel());
        }

        return builder.build();
    }
}
