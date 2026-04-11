package com.photobogota.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.model.Categoria;
import com.photobogota.api.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements ICategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    public List<CategoriaDTO> obtenerTodos() {
        return categoriaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoriaDTO> obtenerActivos() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoriaDTO obtenerPorId(String id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        return toDTO(categoria);
    }

    @Override
    public CategoriaDTO crear(CategoriaDTO dto) {
        if (categoriaRepository.existsByNombre(dto.getNombre())) {
            throw new RuntimeException("El nombre de categoría ya existe");
        }
        Categoria categoria = toEntity(dto);
        categoria = categoriaRepository.save(categoria);
        return toDTO(categoria);
    }

    @Override
    public CategoriaDTO actualizar(String id, CategoriaDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        if (dto.getNombre() != null && !dto.getNombre().equals(categoria.getNombre())) {
            if (categoriaRepository.existsByNombre(dto.getNombre())) {
                throw new RuntimeException("El nombre de categoría ya existe");
            }
            categoria.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            categoria.setDescripcion(dto.getDescripcion());
        }
        if (dto.getImagen() != null) {
            categoria.setImagen(dto.getImagen());
        }
        
        categoria = categoriaRepository.save(categoria);
        return toDTO(categoria);
    }

    @Override
    public void eliminar(String id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada");
        }
        categoriaRepository.deleteById(id);
    }

    @Override
    public CategoriaDTO togglestatus(String id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setActivo(!categoria.getActivo());
        categoria = categoriaRepository.save(categoria);
        return toDTO(categoria);
    }

    private CategoriaDTO toDTO(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setImagen(categoria.getImagen());
        dto.setActivo(categoria.getActivo());
        return dto;
    }

    private Categoria toEntity(CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setImagen(dto.getImagen());
        categoria.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return categoria;
    }
}