package com.photobogota.api.service;

import java.util.List;

import com.photobogota.api.dto.CategoriaDTO;

public interface ICategoriaService {

    List<CategoriaDTO> obtenerTodos();

    List<CategoriaDTO> obtenerActivos();

    CategoriaDTO obtenerPorId(String id);

    CategoriaDTO crear(CategoriaDTO dto);

    CategoriaDTO actualizar(String id, CategoriaDTO dto);

    void eliminar(String id);

    CategoriaDTO togglestatus(String id);
}