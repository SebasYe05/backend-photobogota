package com.photobogota.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.photobogota.api.model.HistorialPuntos;
import com.photobogota.api.model.TipoPuntos;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistorialPuntosRepository extends MongoRepository<HistorialPuntos, String> {

    List<HistorialPuntos> findByUsuarioAndFechaGreaterThanEqual(String usuario, LocalDateTime fecha);

    boolean existsByUsuarioAndTipoAndRefId(String usuario, TipoPuntos tipo, String refId);
}
