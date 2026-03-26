package com.photobogota.api.service;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
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
