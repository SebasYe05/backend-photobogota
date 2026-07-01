package com.photobogota.api.service;

import com.photobogota.api.dto.*;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotService {

    private final SpotRepository spotRepository;
    private final SpotMapper spotMapper;
    private final UsuarioAuthRepository usuarioAuthRepository;

    public List<SpotResumenDTO> obtenerTodos(String categoria, String localidad) {
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

        return spotMapper.toResumenList(spots);
    }

    @Transactional
    public SpotResponseDTO obtenerPorId(String id) {
        Spot spot = spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + id));

        SpotResponseDTO response = spotMapper.toResponse(spot);

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

        return response;
    }

    @Transactional
    public SpotResponseDTO agregarResena(String spotId, ResenaRequestDTO request, String usuario) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + spotId));

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

        return response;
    }
}