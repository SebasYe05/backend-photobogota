package com.photobogota.api.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CanjeResponseDTO;
import com.photobogota.api.dto.CanjearRequestDTO;
import com.photobogota.api.dto.ValidarCanjeRequestDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.RecursoCaducadoException;
import com.photobogota.api.exception.RecursoNoDisponibleException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.CanjeMapper;
import com.photobogota.api.model.Canje;
import com.photobogota.api.model.Promocion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.CanjeRepository;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CanjeService {

    private static final String ESTADO_VIGENTE = "VIGENTE";
    private static final String ESTADO_USADO = "USADO";
    private static final int LARGO_CODIGO = 8;
    private static final char[] ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CanjeRepository canjeRepository;
    private final PromocionRepository promocionRepository;
    private final SpotRepository spotRepository;
    private final CanjeMapper canjeMapper;

    /**
     * Un MEMBRO canjea una promoción activa: genera un código único de 8
     * caracteres, valida vigencia/cupos y la regla de un canje por promoción.
     */
    public CanjeResponseDTO crearCanje(CanjearRequestDTO request, String miembroUsername) {
        Promocion promocion = promocionRepository.findById(request.getPromocionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promoción no encontrada con id: " + request.getPromocionId()));

        if (!Boolean.TRUE.equals(promocion.getActivo()) || !estaDentroDeFechas(promocion)) {
            throw new OperacionInvalidaException("La promoción no está activa en este momento");
        }

        Integer usos = promocion.getUsos() != null ? promocion.getUsos() : 0;
        if (promocion.getUsosMaximos() != null && usos >= promocion.getUsosMaximos()) {
            throw new RecursoNoDisponibleException("La promoción está agotada");
        }

        // 409 si el miembro ya tiene un canje VIGENTE o USADO para esta promoción.
        boolean yaCanjeado = canjeRepository.findByPromocionIdAndMiembroNombre(promocion.getId(), miembroUsername)
                .stream()
                .anyMatch(this::esConflictivo);
        if (yaCanjeado) {
            throw new RecursoNoDisponibleException("Ya tienes un canje vigente o usado para esta promoción");
        }

        Canje canje = Canje.builder()
                .codigo(generarCodigoUnico())
                .estado(ESTADO_VIGENTE)
                .fechaCanje(LocalDateTime.now())
                .fechaExpiracion(promocion.getFechaFin())
                .promocionId(promocion.getId())
                .promocionTitulo(promocion.getTitulo())
                .descuento(promocion.getDescuento())
                .spotId(promocion.getSpotId())
                .spotNombre(promocion.getNombreSpot())
                .miembroNombre(miembroUsername)
                .socioUsername(promocion.getSocioUsername())
                .build();

        promocion.setUsos(usos + 1);
        promocionRepository.save(promocion);

        return canjeMapper.toResponse(canjeRepository.save(canje));
    }

    public List<CanjeResponseDTO> listarMios(String miembroUsername) {
        return canjeRepository.findByMiembroNombre(miembroUsername).stream()
                .map(canjeMapper::toResponse)
                .toList();
    }

    public List<CanjeResponseDTO> listarDePromocion(String promocionId, String socioUsername) {
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + promocionId));
        if (!socioUsername.equalsIgnoreCase(promocion.getSocioUsername())) {
            throw new AccessForbiddenException("Solo el socio dueño puede consultar los canjes de esta promoción");
        }
        return canjeRepository.findByPromocionId(promocionId).stream()
                .map(canjeMapper::toResponse)
                .toList();
    }

    /**
     * El SOCIO dueño del local valida el código presentado por el miembro:
     * verifica spot propio, que el código exista y esté vigente, que no esté
     * vencido y que la promoción siga activa; luego lo marca como USADO.
     */
    public CanjeResponseDTO validarCanje(ValidarCanjeRequestDTO request, String socioUsername) {
        Spot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con id: " + request.getSpotId()));
        if (!socioUsername.equalsIgnoreCase(spot.getCreadorUsername())) {
            throw new AccessForbiddenException("Solo el socio dueño del local puede validar canjes");
        }

        Canje canje = canjeRepository.findByCodigoIgnoreCase(request.getCodigo())
                .orElseThrow(() -> new ResourceNotFoundException("Código de canje no encontrado"));

        if (!canje.getSpotId().equalsIgnoreCase(request.getSpotId())) {
            throw new OperacionInvalidaException("El código no corresponde a este local");
        }

        if (ESTADO_USADO.equals(canje.getEstado())) {
            throw new RecursoNoDisponibleException("Este código ya fue utilizado");
        }

        if (canje.getFechaExpiracion() != null && canje.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RecursoCaducadoException("Este código está vencido");
        }

        Promocion promocion = promocionRepository.findById(canje.getPromocionId())
                .orElseThrow(() -> new ResourceNotFoundException("La promoción del canje ya no existe"));
        if (!Boolean.TRUE.equals(promocion.getActivo()) || !estaDentroDeFechas(promocion)) {
            throw new OperacionInvalidaException("La promoción no está activa, no se puede validar el código");
        }

        canje.setEstado(ESTADO_USADO);
        return canjeMapper.toResponse(canjeRepository.save(canje));
    }

    /**
     * Un canje anterior bloquea uno nuevo solo si sigue vigente (VIGENTE y no
     * vencido) o si ya fue usado. Un canje EXPIRADO no bloquea.
     */
    private boolean esConflictivo(Canje canje) {
        if (ESTADO_USADO.equals(canje.getEstado())) {
            return true;
        }
        return ESTADO_VIGENTE.equals(canje.getEstado())
                && (canje.getFechaExpiracion() == null || !canje.getFechaExpiracion().isBefore(LocalDateTime.now()));
    }

    private boolean estaDentroDeFechas(Promocion promocion) {
        LocalDateTime ahora = LocalDateTime.now();
        return (promocion.getFechaInicio() == null || !promocion.getFechaInicio().isAfter(ahora))
                && (promocion.getFechaFin() == null || !promocion.getFechaFin().isBefore(ahora));
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(LARGO_CODIGO);
            for (int i = 0; i < LARGO_CODIGO; i++) {
                sb.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
            }
            codigo = sb.toString();
        } while (canjeRepository.findByCodigoIgnoreCase(codigo).isPresent());
        return codigo;
    }
}