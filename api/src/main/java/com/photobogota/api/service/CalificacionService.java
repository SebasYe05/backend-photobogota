package com.photobogota.api.service;

import com.photobogota.api.dto.CalificacionRequestDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final SpotRepository spotRepository;

    @Transactional
    public CalificacionResponseDTO crearCalificacion(String spotId, CalificacionRequestDTO request, String usuario) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + spotId));

        Calificacion calificacion = new Calificacion();
        calificacion.setSpotId(spotId);
        calificacion.setUsuario(usuario);
        calificacion.setEstrellas(request.getEstrellas());
        calificacion.setComentario(request.getComentario());

        calificacionRepository.save(calificacion);
        log.info("Calificacion creada para spot {} por usuario {}", spotId, usuario);

        recalcularRatingSpot(spotId);

        return CalificacionResponseDTO.from(calificacion);
    }

    public List<CalificacionResponseDTO> listarPorSpot(String spotId) {
        return calificacionRepository.findBySpotId(spotId)
                .stream()
                .map(CalificacionResponseDTO::from)
                .collect(Collectors.toList());
    }

    public CalificacionResponseDTO obtenerPorId(String id) {
        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calificacion no encontrada con id: " + id));
        return CalificacionResponseDTO.from(calificacion);
    }

    private void recalcularRatingSpot(String spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot no encontrado con id: " + spotId));

        List<Calificacion> calificaciones = calificacionRepository.findBySpotId(spotId);

        double promedio = calificaciones.stream()
                .mapToInt(Calificacion::getEstrellas)
                .average()
                .orElse(0.0);

        spot.setRating(Math.round(promedio * 10.0) / 10.0);
        spot.setTotalResenas(calificaciones.size());
        spotRepository.save(spot);
        log.info("Rating recalculado para spot {}: {} ({} calificaciones)", spotId, spot.getRating(), calificaciones.size());
    }
}
