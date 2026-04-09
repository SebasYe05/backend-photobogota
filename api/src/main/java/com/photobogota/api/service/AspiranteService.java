package com.photobogota.api.service;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.exception.AspiranteAlreadyExistsException;
import com.photobogota.api.mapper.AspiranteMapper;
import com.photobogota.api.model.Aspirante;
import com.photobogota.api.repository.AspiranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AspiranteService {

    private final AspiranteRepository AspiranteRepository;
    private final AspiranteMapper AspiranteMapper;

    public AspiranteResponseDTO crearSolicitud(SolicitudAspiranteDTO request) {
        if (AspiranteRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AspiranteAlreadyExistsException("Ya existe una solicitud con este email");
        }

        if (AspiranteRepository.findByNit(request.getNit()).isPresent()) {
            throw new AspiranteAlreadyExistsException("Ya existe una solicitud con este NIT/RUT");
        }

        Aspirante aspirante = new Aspirante();
        aspirante.setNombres(request.getNombres());
        aspirante.setApellidos(request.getApellidos());
        aspirante.setEmail(request.getEmail());
        aspirante.setTelefono(request.getTelefono());
        aspirante.setDireccion(request.getDireccion());
        aspirante.setNit(request.getNit());
        aspirante.setFechaNacimiento(request.getFechaNacimiento());
        aspirante.setNombrePropietario(request.getNombrePropietario());
        aspirante.setRazonSocial(request.getRazonSocial());
        aspirante.setCategoria(request.getCategoria());
        aspirante.setLocalidad(request.getLocalidad());
        aspirante.setRutaArchivo(request.getRutaArchivo());
        aspirante.setTipoArchivo(request.getTipoArchivo());
        aspirante.setEstado("PENDIENTE");
        aspirante.setFechaSolicitud(LocalDate.now());

        return AspiranteMapper.toResponse(AspiranteRepository.save(aspirante));
    }

    public AspiranteResponseDTO obtenerPorId(String id) {
        Aspirante Aspirante = AspiranteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con id: " + id));
        return AspiranteMapper.toResponse(Aspirante);
    }

    public AspiranteResponseDTO obtenerPorEmail(String email) {
        Aspirante Aspirante = AspiranteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con email: " + email));
        return AspiranteMapper.toResponse(Aspirante);
    }

    public List<AspiranteResponseDTO> obtenerTodos() {
        return AspiranteMapper.toResponseList(AspiranteRepository.findAll());
    }
}