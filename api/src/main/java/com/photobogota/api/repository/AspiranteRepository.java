package com.photobogota.api.repository;

import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AspiranteRepository extends MongoRepository<Aspirante, String> {

    // Nota: ya no asumimos un único documento por email/nit, porque un
    // aspirante rechazado puede volver a aplicar pasados 90 días y eso crea
    // un nuevo documento histórico. Por eso estas consultas retornan listas
    // ordenadas por fecha, y el servicio decide con cuál trabajar.
    List<Aspirante> findByEmailOrderByFechaSolicitudDesc(String email);

    List<Aspirante> findByNitOrderByFechaSolicitudDesc(String nit);

    Optional<Aspirante> findByCodigo(String codigo);

    List<Aspirante> findByEstado(EstadoAspirante estado);
}
