package com.photobogota.api.service;

import com.photobogota.api.dto.*;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotService {

    private final SpotRepository spotRepository;
    private final SpotMapper spotMapper;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final INotificacionService notificacionService;
    private final IPuntosService puntosService;
    private final IFiltroContenidoService filtroContenidoService;
    private final PromocionService promocionService;

    public List<SpotResumenDTO> obtenerTodos(
            String categoria,
            String localidad,
            String tipo,
            String nombre,
            Boolean mios,
            String username) {
        List<Spot> spots;

        if (categoria != null && localidad != null) {
            spots = spotRepository.findByCategoriaAndLocalidad(categoria, localidad);
        } else if (categoria != null) {
            spots = spotRepository.findByCategoria(categoria);
        } else if (localidad != null) {
            spots = spotRepository.findByLocalidad(localidad);
        } else {
            spots = spotRepository.findAll();
        }

        List<Spot> filtrados = spots.stream()
                .filter(spot -> nombre == null || nombre.isBlank()
                        || (spot.getNombre() != null
                                && spot.getNombre().toLowerCase().contains(nombre.toLowerCase())))
                .filter(spot -> tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase(normalizarTipo(spot)))
                .filter(spot -> !Boolean.TRUE.equals(mios)
                        || (username != null && username.equalsIgnoreCase(spot.getCreadorUsername())))
                .toList();

        List<SpotResumenDTO> resumenes = spotMapper.toResumenList(filtrados);

        // Enriquecer con la existencia de promociones vigentes: el mapa usa este
        // flag para cambiar el icono del local. Se calcula una sola vez.
        Set<String> conPromocion = promocionService.obtenerSpotIdsConPromocionActiva();
        resumenes.forEach(r -> r.setTienePromocion(conPromocion.contains(r.getId())));

        return resumenes;
    }

    @Transactional
    public SpotResponseDTO obtenerPorId(String id) {
        Spot spot = spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + id));

        SpotResponseDTO response = spotMapper.toResponse(spot);

        response.setTienePromocion(promocionService.tienePromocionActiva(id));

        // Si no tiene rol pero tiene username, intentamos obtenerlo
        if (spot.getCreadorRol() == null && spot.getCreadorUsername() != null) {
            usuarioAuthRepository.findByEmailOrNombreUsuario(spot.getCreadorUsername(), spot.getCreadorUsername())
                    .ifPresentOrElse(
                            usuario -> {
                                response.setRol(usuario.getRol().name());
                                spot.setCreadorRol(usuario.getRol().name());
                                spotRepository.save(spot);
                            },
                            () -> log.warn("UsuarioAuth no encontrado para creadorUsername: '{}'",
                                    spot.getCreadorUsername()));
        } else if (spot.getCreadorRol() != null) {
            // Si ya tiene rol, lo seteamos en la respuesta
            response.setRol(spot.getCreadorRol());
        }

        return response;
    }

    @Transactional
    public SpotResponseDTO crearSpot(CrearSpotRequestDTO request, String creadorUsername, String rol) {
        filtroContenidoService.validarContenido(creadorUsername, TipoContenidoModerado.SPOT_NOMBRE,
                request.getNombre());
        filtroContenidoService.validarContenido(creadorUsername, TipoContenidoModerado.SPOT_DESCRIPCION,
                request.getDescripcion());

        Spot spot = new Spot();
        spot.setNombre(request.getNombre());
        spot.setLatitud(request.getLatitud());
        spot.setLongitud(request.getLongitud());
        spot.setDireccion(request.getDireccion());
        spot.setCategoria(request.getCategoria());
        spot.setLocalidad(request.getLocalidad());
        spot.setDescripcion(request.getDescripcion());
        spot.setRecomendacion(request.getRecomendacion());
        spot.setTipsFoto(request.getTipsFoto());
        spot.setCreadorUsername(creadorUsername);
        spot.setCreadorRol(rol);

        // Contrato LOCAL: si el front envía tipo lo respetamos (solo un SOCIO
        // puede registrar LOCALES); si no viene, lo derivamos del rol para que
        // los spots antiguos se comporten igual (SOCIO -> LOCAL, resto -> SPOT).
        String tipoNormalizado = request.getTipo() != null ? request.getTipo()
                : ("SOCIO".equals(rol) ? "LOCAL" : "SPOT");
        if (!"SOCIO".equals(rol) && "LOCAL".equals(tipoNormalizado)) {
            tipoNormalizado = "SPOT";
        }
        spot.setTipo(tipoNormalizado);
        spot.setTelefono(request.getTelefono());
        spot.setHorario(request.getHorario());
        spot.setSitioWeb(request.getSitioWeb());

        if (request.getImagenes() != null && !request.getImagenes().isEmpty()) {
            spot.setImagenes(request.getImagenes());
        }

        Spot savedSpot = spotRepository.save(spot);
        SpotResponseDTO response = spotMapper.toResponse(savedSpot);

        // Obtener el rol del usuario para la respuesta
        usuarioAuthRepository.findByEmailOrNombreUsuario(creadorUsername, creadorUsername)
                .ifPresentOrElse(
                        usuario -> response.setRol(usuario.getRol().name()),
                        () -> log.warn("UsuarioAuth no encontrado para creadorUsername: '{}'", creadorUsername));

        try {
            notificacionService.notificarNuevoSpot(savedSpot);
        } catch (Exception e) {
            log.error("No se pudo notificar el nuevo spot {}: {}", savedSpot.getId(), e.getMessage());
        }

        try {
            if ("MIEMBRO".equals(rol)) {
                puntosService.sumarPuntos(creadorUsername,
                        com.photobogota.api.model.TipoPuntos.CREAR_SPOT, savedSpot.getId());
            }
        } catch (Exception e) {
            log.error("No se pudo otorgar puntos por crear spot {}: {}", savedSpot.getId(), e.getMessage());
        }

        return response;
    }

    @Transactional
    public SpotResponseDTO agregarResena(String spotId, ResenaRequestDTO request, String usuario) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + spotId));

        filtroContenidoService.validarContenido(usuario, TipoContenidoModerado.RESENA, request.getComentario());

        Spot.Resena resena = new Spot.Resena();
        resena.setId(UUID.randomUUID().toString());
        resena.setUsuario(usuario);
        resena.setRating(request.getRating());
        resena.setComentario(request.getComentario());

        spot.getResenas().add(resena);

        // Recalcular rating promedio
        double nuevoRating = spot.getResenas().stream()
                .mapToInt(r -> r.getRating()) 
                .average()
                .orElse(0.0);

        spot.setRating(Math.round(nuevoRating * 10.0) / 10.0);
        spot.setTotalResenas(spot.getResenas().size());

        Spot updatedSpot = spotRepository.save(spot);
        SpotResponseDTO response = spotMapper.toResponse(updatedSpot);

        // Asegurar que el rol esté presente en la respuesta
        if (updatedSpot.getCreadorRol() != null) {
            response.setRol(updatedSpot.getCreadorRol());
        }

        response.setTienePromocion(promocionService.tienePromocionActiva(spotId));

        try {
            notificacionService.notificarNuevaResena(updatedSpot, resena, usuario);
        } catch (Exception e) {
            log.error("No se pudo notificar la nueva reseña en el spot {}: {}", spotId, e.getMessage());
        }

        return response;
    }

    private String normalizarTipo(Spot spot) {
        if (spot == null) return "SPOT";
        if (spot.getTipo() != null && !spot.getTipo().isBlank()) {
            return spot.getTipo();
        }
        return "SOCIO".equals(spot.getCreadorRol()) ? "LOCAL" : "SPOT";
    }
}