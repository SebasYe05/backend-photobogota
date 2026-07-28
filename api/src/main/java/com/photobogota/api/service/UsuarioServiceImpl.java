package com.photobogota.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.ResenaDTO;
import com.photobogota.api.dto.SpotResponseDTO;
import com.photobogota.api.exception.CambioContrasenaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Guardado;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.GuardadoRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;
import com.photobogota.api.repository.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpotRepository spotRepository;
    private final SpotMapper spotMapper;
    private final GuardadoRepository guardadoRepository;

    @Override
    public PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto) {
        log.info("Editando perfil del usuario: {}", nombreUsuario);

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de usuario no encontrado"));

        if (dto.getNombresCompletos() != null) {
            usuario.setNombresCompletos(dto.getNombresCompletos());
        }
        if (dto.getTelefono() != null) {
            usuario.setTelefono(dto.getTelefono());
        }
        if (dto.getFotoPerfil() != null) {
            usuario.setFotoPerfil(dto.getFotoPerfil());
        }
        if (dto.getBiografia() != null) {
            usuario.setBiografia(dto.getBiografia());
        }

        usuarioRepository.save(usuario);

        log.info("Perfil actualizado exitosamente para: {}", nombreUsuario);

        return construirPerfilDTO(usuario, usuarioAuth);
    }

    @Override
    public PerfilUsuarioDTO obtenerPerfil(String nombreUsuario) {
        log.info("Obteniendo perfil del usuario: {}", nombreUsuario);

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de usuario no encontrado"));

        return construirPerfilDTO(usuario, usuarioAuth);
    }

    @Override
    public CambiarContrasenaResponseDTO cambiarContrasena(String nombreUsuario, CambiarContrasenaDTO dto) {
        log.info("Solicitando cambio de contraseña para el usuario: {}", nombreUsuario);

        if (!dto.getNuevaContrasena().equals(dto.getConfirmarContrasena())) {
            throw new CambioContrasenaException("La nueva contraseña y la confirmación no coinciden");
        }

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new CambioContrasenaException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getContrasenaActual(), usuarioAuth.getContrasena())) {
            throw new CambioContrasenaException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(dto.getNuevaContrasena(), usuarioAuth.getContrasena())) {
            throw new CambioContrasenaException("La nueva contraseña debe ser diferente a la actual");
        }

        usuarioAuth.setContrasena(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuarioAuthRepository.save(usuarioAuth);

        log.info("Contraseña actualizada exitosamente para el usuario: {}", nombreUsuario);

        return CambiarContrasenaResponseDTO.builder()
                .mensaje("Contraseña actualizada exitosamente")
                .build();
    }

    @Override
    public List<SpotResponseDTO> obtenerSpotsDeUsuario(String nombreUsuario) {
        log.info("Obteniendo spots del usuario: {}", nombreUsuario);

        usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Spot> spots = spotRepository.findByCreadorUsername(nombreUsuario);
        return spots.stream()
                .map(spotMapper::toResponse)
                .toList();
    }

    @Override
    public List<ResenaDTO> obtenerResenasDeUsuario(String nombreUsuario) {
        log.info("Obteniendo reseñas del usuario: {}", nombreUsuario);

        usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Spot> spots = spotRepository.findByResenasUsuario(nombreUsuario);
        List<ResenaDTO> result = new java.util.ArrayList<>();

        for (Spot spot : spots) {
            for (Spot.Resena resena : spot.getResenas()) {
                if (nombreUsuario.equals(resena.getUsuario())) {
                    ResenaDTO dto = new ResenaDTO();
                    dto.setId(resena.getId());
                    dto.setSpotId(spot.getId());
                    dto.setTituloSpot(spot.getNombre());
                    dto.setRating(resena.getRating());
                    dto.setTexto(resena.getComentario());
                    dto.setLikes(0);
                    dto.setFechaCreacion(resena.getFecha());
                    result.add(dto);
                }
            }
        }

        return result;
    }

    @Override
    public List<SpotResponseDTO> obtenerGuardados(String nombreUsuario) {
        log.info("Obteniendo spots guardados del usuario: {}", nombreUsuario);

        List<Guardado> guardados = guardadoRepository.findByNombreUsuario(nombreUsuario);
        List<SpotResponseDTO> result = new java.util.ArrayList<>();

        for (Guardado guardado : guardados) {
            spotRepository.findById(guardado.getSpotId()).ifPresent(spot -> {
                result.add(spotMapper.toResponse(spot));
            });
        }

        return result;
    }

    private PerfilUsuarioDTO construirPerfilDTO(Usuario usuario, UsuarioAuth usuarioAuth) {
        PerfilUsuarioDTO.PerfilUsuarioDTOBuilder builder = PerfilUsuarioDTO.builder()
                .id(usuario.getId())
                .nombresCompletos(usuario.getNombresCompletos())
                .email(usuarioAuth.getEmail())
                .nombreUsuario(usuarioAuth.getNombreUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .biografia(usuario.getBiografia())
                .tipoUsuario(usuarioAuth.getRol().name())
                .rol(usuarioAuth.getRol().name());

        if (usuario instanceof Miembro) {
            Miembro miembro = (Miembro) usuario;
            builder.puntos(miembro.getPuntos())
                    .nivel(miembro.getNivel());
        }

        long totalSpots = spotRepository.countByCreadorUsername(usuarioAuth.getNombreUsuario());
        long totalResenas = spotRepository.findByResenasUsuario(usuarioAuth.getNombreUsuario()).size();
        long totalGuardados = guardadoRepository.countByNombreUsuario(usuarioAuth.getNombreUsuario());

        builder.totalSpots(totalSpots)
                .totalResenas(totalResenas)
                .totalGuardados(totalGuardados);

        return builder.build();
    }
}
