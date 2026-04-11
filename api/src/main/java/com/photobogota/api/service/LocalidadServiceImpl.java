package com.photobogota.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.model.Localidades;
import com.photobogota.api.repository.LocalidadesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalidadServiceImpl implements ILocalidadService {

    private final LocalidadesRepository localidadesRepository;

    @Override
    public List<LocalidadDTO> obtenerTodos() {
        return localidadesRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocalidadDTO> obtenerActivos() {
        return localidadesRepository.findByActivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LocalidadDTO obtenerPorId(String id) {
        Localidades localidad = localidadesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));
        return toDTO(localidad);
    }

    @Override
    public LocalidadDTO crear(LocalidadDTO dto) {
        if (localidadesRepository.existsByNombre(dto.getNombre())) {
            throw new RuntimeException("El nombre de localidad ya existe");
        }
        Localidades localidad = toEntity(dto);
        localidad = localidadesRepository.save(localidad);
        return toDTO(localidad);
    }

    @Override
    public LocalidadDTO actualizar(String id, LocalidadDTO dto) {
        Localidades localidad = localidadesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));
        
        if (dto.getNombre() != null && !dto.getNombre().equals(localidad.getNombre())) {
            if (localidadesRepository.existsByNombre(dto.getNombre())) {
                throw new RuntimeException("El nombre de localidad ya existe");
            }
            localidad.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            localidad.setDescripcion(dto.getDescripcion());
        }
        if (dto.getImagen() != null) {
            localidad.setImagen(dto.getImagen());
        }
        
        localidad = localidadesRepository.save(localidad);
        return toDTO(localidad);
    }

    @Override
    public void eliminar(String id) {
        if (!localidadesRepository.existsById(id)) {
            throw new RuntimeException("Localidad no encontrada");
        }
        localidadesRepository.deleteById(id);
    }

    @Override
    public LocalidadDTO toggleStatus(String id) {
        Localidades localidad = localidadesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));
        localidad.setActivo(!localidad.getActivo());
        localidad = localidadesRepository.save(localidad);
        return toDTO(localidad);
    }

    private LocalidadDTO toDTO(Localidades localidad) {
        LocalidadDTO dto = new LocalidadDTO();
        dto.setId(localidad.getId());
        dto.setNombre(localidad.getNombre());
        dto.setDescripcion(localidad.getDescripcion());
        dto.setImagen(localidad.getImagen());
        dto.setActivo(localidad.getActivo());
        return dto;
    }

    private Localidades toEntity(LocalidadDTO dto) {
        Localidades localidad = new Localidades();
        localidad.setNombre(dto.getNombre());
        localidad.setDescripcion(dto.getDescripcion());
        localidad.setImagen(dto.getImagen());
        localidad.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return localidad;
    }
}