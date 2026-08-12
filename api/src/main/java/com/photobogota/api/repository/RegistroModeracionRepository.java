package com.photobogota.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.EstadoApelacion;
import com.photobogota.api.model.RegistroModeracion;

public interface RegistroModeracionRepository extends MongoRepository<RegistroModeracion, String> {

    List<RegistroModeracion> findByUsuarioIdOrderByFechaDesc(String usuarioId);

    List<RegistroModeracion> findByAccionAndEstadoApelacionOrderByFechaDesc(AccionModeracion accion,
            EstadoApelacion estadoApelacion);

    List<RegistroModeracion> findByAccionInAndEstadoApelacionOrderByFechaDesc(List<AccionModeracion> accions,
            EstadoApelacion estadoApelacion);
}
