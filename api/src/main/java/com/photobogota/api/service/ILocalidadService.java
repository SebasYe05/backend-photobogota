package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.LocalidadDTO;

public interface ILocalidadService {

    List<LocalidadDTO> obtenerTodos();

    List<LocalidadDTO> obtenerActivos();

    LocalidadDTO obtenerPorId(String id);

    LocalidadDTO crear(LocalidadDTO dto);

    LocalidadDTO actualizar(String id, LocalidadDTO dto);

    void eliminar(String id);

    LocalidadDTO toggleStatus(String id);
}