package com.photobogota.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.photobogota.api.model.MantenimientoProgramado;

public interface MantenimientoRepository extends MongoRepository<MantenimientoProgramado, String> {

    // Ventana activa justo ahora (usada por el filtro y por /mantenimiento/estado)
    Optional<MantenimientoProgramado> findFirstByCanceladoFalseAndFechaInicioBeforeAndFechaFinAfterOrderByFechaInicioDesc(
            LocalDateTime ahoraParaInicio, LocalDateTime ahoraParaFin);

    // Próximo mantenimiento programado que todavía no ha iniciado (para avisar con antelación en el front)
    Optional<MantenimientoProgramado> findFirstByCanceladoFalseAndFechaInicioAfterOrderByFechaInicioAsc(
            LocalDateTime ahora);

    // Usados por el scheduler para detectar ventanas que acaban de iniciar/terminar
    List<MantenimientoProgramado> findByCanceladoFalseAndAvisoInicioEnviadoFalseAndFechaInicioBefore(
            LocalDateTime ahora);

    List<MantenimientoProgramado> findByCanceladoFalseAndAvisoFinEnviadoFalseAndFechaFinBefore(
            LocalDateTime ahora);

    // Listado para la UI de admin: todos los programados (activos, futuros o recién pasados) sin cancelar
    List<MantenimientoProgramado> findByCanceladoFalseOrderByFechaInicioDesc();
}
