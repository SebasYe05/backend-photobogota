package com.photobogota.api.service;

import com.photobogota.api.dto.*;
import com.photobogota.api.exception.UnauthorizedException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpotService {

    private final SpotRepository spotRepository;
    private final SpotMapper spotMapper;

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

    public SpotResponseDTO obtenerPorId(String id) {
        Spot spot = spotRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Spot no encontrado con id: " + id));

        return spotMapper.toResponse(spot);
    }

    public SpotResponseDTO crearSpot(CrearSpotRequestDTO request, String creadorUsername) {
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

        if (request.getImagenes() != null && !request.getImagenes().isEmpty()) {
            spot.setImagenes(request.getImagenes());
        }

        return spotMapper.toResponse(spotRepository.save(spot));
    }

    public SpotResponseDTO agregarResena(String spotId, ResenaRequestDTO request, String usuario) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new UnauthorizedException("Spot no encontrado con id: " + spotId));

        Spot.Resena resena = new Spot.Resena();
        resena.setId(UUID.randomUUID().toString());
        resena.setUsuario(usuario);
        resena.setRating(request.getRating());
        resena.setComentario(request.getComentario());

        spot.getResenas().add(resena);

        // Recalcular rating promedio
        double nuevoRating = spot.getResenas().stream()
                .mapToInt(Spot.Resena::getRating)
                .average()
                .orElse(0.0);

        spot.setRating(Math.round(nuevoRating * 10.0) / 10.0);
        spot.setTotalResenas(spot.getResenas().size());

        return spotMapper.toResponse(spotRepository.save(spot));
    }
}