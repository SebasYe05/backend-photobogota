package com.photobogota.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.Notificacion;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    Page<Notificacion> findByDestinatarioUsernameOrderByFechaCreacionDesc(String destinatarioUsername,
            Pageable pageable);

    Page<Notificacion> findByDestinatarioUsernameAndLeidaOrderByFechaCreacionDesc(String destinatarioUsername,
            Boolean leida, Pageable pageable);

    long countByDestinatarioUsernameAndLeidaFalse(String destinatarioUsername);

    List<Notificacion> findByDestinatarioUsernameAndLeidaFalse(String destinatarioUsername);
}
