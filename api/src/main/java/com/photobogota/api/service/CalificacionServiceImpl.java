package com.photobogota.api.service;

import com.photobogota.api.dto.CalificacionRequestDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.exception.ResourceAlreadyExistsException;
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
public class CalificacionServiceImpl implements ICalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final SpotRepository spotRepository;

    @Override
    @Transactional
    public CalificacionResponseDTO crearCalificacion(String spotId, CalificacionRequestDTO request, String usuario) {
        if (!spotRepository.existsById(spotId)) {
            throw new ResourceNotFoundException("Spot no encontrado con id: " + spotId);
        }

        if (calificacionRepository.findBySpotIdAndUsuario(spotId, usuario) != null) {
            throw new ResourceAlreadyExistsException("calificacion para este spot", usuario);
        }

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

    @Override
    @Transactional
    public CalificacionResponseDTO modificarCalificacion(String spotId, String calificacionId, CalificacionRequestDTO request, String usuario) {
        Calificacion calificacion = calificacionRepository.findById(calificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Calificacion no encontrada con id: " + calificacionId));

        if (!calificacion.getSpotId().equals(spotId)) {
            throw new ResourceNotFoundException("La calificacion no pertenece al spot indicado");
        }

        if (!calificacion.getUsuario().equals(usuario)) {
            throw new ResourceNotFoundException("No tienes permiso para modificar esta calificacion");
        }

        calificacion.setEstrellas(request.getEstrellas());
        calificacion.setComentario(request.getComentario());

        calificacionRepository.save(calificacion);
        log.info("Calificacion {} modificada para spot {} por usuario {}", calificacionId, spotId, usuario);

        recalcularRatingSpot(spotId);

        return CalificacionResponseDTO.from(calificacion);
    }

    @Override
    public List<CalificacionResponseDTO> listarPorSpot(String spotId) {
        return calificacionRepository.findBySpotId(spotId)
                .stream()
                .map(CalificacionResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
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
                .mapToInt(calificacion -> calificacion.getEstrellas())
                .average()
                .orElse(0.0);

        spot.setRating(Math.round(promedio * 10.0) / 10.0);
        spot.setTotalResenas(calificaciones.size());
        spotRepository.save(spot);
        log.info("Rating recalculado para spot {}: {} ({} calificaciones)", spotId, spot.getRating(), calificaciones.size());
    }
}
