package com.photobogota.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.exception.CambioContrasenaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.Guardado;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.GuardadoRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpotRepository spotRepository;
    private final CalificacionRepository calificacionRepository;
    private final GuardadoRepository guardadoRepository;
    private final SpotMapper spotMapper;

    @Override
    public PerfilUsuarioDTO editarPerfil(String nombreUsuario, EditarPerfilDTO dto) {
        log.info("Editando perfil del usuario: {}", nombreUsuario);

        // Buscar el usuario en la colección de auth por nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Buscar el perfil del usuario en la colección de usuarios
        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Actualizar los campos proporcionados
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Buscar el perfil del usuario en la colección de usuarios
        Usuario usuario = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

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

    @Override
    public List<SpotResumenDTO> obtenerSpotsDeUsuario(String nombreUsuario) {
        log.info("Obteniendo spots del usuario: {}", nombreUsuario);
        if (!usuarioAuthRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        List<Spot> spots = spotRepository.findByCreadorUsername(nombreUsuario);
        List<SpotResumenDTO> resumen = spotMapper.toResumenList(spots);
        return resumen != null ? resumen : Collections.emptyList();
    }

    @Override
    public List<CalificacionResponseDTO> obtenerResenasDeUsuario(String nombreUsuario) {
        log.info("Obteniendo reseñas del usuario: {}", nombreUsuario);
        if (!usuarioAuthRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        List<Calificacion> calificaciones = calificacionRepository.findByUsuario(nombreUsuario);
        if (calificaciones == null || calificaciones.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch-fetch de los spots padre para poblar tituloSpot (contrato del front)
        List<String> spotIds = calificaciones.stream()
                .map(calificacion -> calificacion.getSpotId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> tituloPorSpotId = new java.util.HashMap<>();
        spotRepository.findAllById(spotIds)
                .forEach(spot -> tituloPorSpotId.put(spot.getId(), spot.getNombre() != null ? spot.getNombre() : ""));

        return calificaciones.stream()
                .map(c -> CalificacionResponseDTO.from(c, tituloPorSpotId.get(c.getSpotId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpotResumenDTO> obtenerGuardados(String nombreUsuario) {
        log.info("Obteniendo guardados del usuario: {}", nombreUsuario);
        List<Guardado> guardados = guardadoRepository.findByUsuario(nombreUsuario);
        if (guardados == null || guardados.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> spotIds = guardados.stream()
                .map(guardado -> guardado.getSpotId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Spot> spots = new java.util.ArrayList<>();
        spotRepository.findAllById(spotIds).forEach(spots::add);
        List<SpotResumenDTO> resumen = spotMapper.toResumenList(spots);
        return resumen != null ? resumen : Collections.emptyList();
    }

    @Override
    public SpotResumenDTO guardarSpot(String nombreUsuario, String spotId) {
        log.info("Guardando spot {} para el usuario {}", spotId, nombreUsuario);

        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado"));

        // Idempotente: si ya estaba guardado, no duplicamos, solo devolvemos el spot
        if (!guardadoRepository.existsByUsuarioAndSpotId(nombreUsuario, spotId)) {
            Guardado guardado = Guardado.builder()
                    .usuario(nombreUsuario)
                    .spotId(spot.getId())
                    .guardadoEn(java.time.LocalDateTime.now())
                    .build();
            guardadoRepository.save(guardado);
        }

        return spotMapper.toResumen(spot);
    }

    @Override
    public void quitarGuardado(String nombreUsuario, String spotId) {
        log.info("Quitando guardado de spot {} para el usuario {}", spotId, nombreUsuario);
        // Idempotente: si no existía ningún guardado, simplemente no hace nada (no falla)
        guardadoRepository.findByUsuario(nombreUsuario).stream()
                .filter(g -> g.getSpotId().equals(spotId))
                .forEach(guardadoRepository::delete);
    }

    /**
     * Método auxiliar para construir el PerfilUsuarioDTO
     */
    private PerfilUsuarioDTO construirPerfilDTO(Usuario usuario, UsuarioAuth usuarioAuth) {
        String nombreUsuario = usuarioAuth.getNombreUsuario();

        PerfilUsuarioDTO.PerfilUsuarioDTOBuilder builder = PerfilUsuarioDTO.builder()
                .id(usuario.getId())
                .nombresCompletos(usuario.getNombresCompletos())
                .email(usuarioAuth.getEmail())
                .nombreUsuario(nombreUsuario)
                .telefono(usuario.getTelefono())
                .fotoPerfil(usuario.getFotoPerfil())
                .biografia(usuario.getBiografia())
                .rol(usuarioAuth.getRol().name())
                .totalSpots((int) spotRepository.countByCreadorUsername(nombreUsuario))
                .totalResenas((int) calificacionRepository.countByUsuario(nombreUsuario))
                .totalGuardados((int) guardadoRepository.countByUsuario(nombreUsuario));

        // Solo los MIEMBRO tienen puntos/nivel; para el resto de roles queda en null
        if (usuario instanceof Miembro) {
            Miembro miembro = (Miembro) usuario;
            builder.puntos(miembro.getPuntos())
                    .nivel(miembro.getNivel());
        }

        return builder.build();
    }
}
