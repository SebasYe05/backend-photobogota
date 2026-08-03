package com.photobogota.api.mapper;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.ComentarioInternoDTO;
import com.photobogota.api.model.Aspirante;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AspiranteMapper {

    public AspiranteResponseDTO toResponse(Aspirante aspirante) {
        return AspiranteResponseDTO.builder()
                .id(aspirante.getId() != null ? aspirante.getId().toString() : null)
                .nombres(aspirante.getNombres())
                .apellidos(aspirante.getApellidos())
                .email(aspirante.getEmail())
                .telefono(aspirante.getTelefono())
                .direccion(aspirante.getDireccion())
                .nit(aspirante.getNit())
                .fechaNacimiento(aspirante.getFechaNacimiento())
                .nombrePropietario(aspirante.getNombrePropietario())
                .razonSocial(aspirante.getRazonSocial())
                .categoria(aspirante.getCategoria())
                .localidad(aspirante.getLocalidad())
                .rutaArchivo(aspirante.getRutaArchivo())
                .tipoArchivo(aspirante.getTipoArchivo())
                .estado(aspirante.getEstado())
                .fechaSolicitud(aspirante.getFechaSolicitud())
                .codigo(aspirante.getCodigo())
                .motivoDecision(aspirante.getMotivoDecision())
                .decididoPor(aspirante.getDecididoPor())
                .fechaDecision(aspirante.getFechaDecision())
                .fechaReenvio(aspirante.getFechaReenvio())
                .vecesCorregida(aspirante.getVecesCorregida())
                .comentariosInternos(mapearComentarios(aspirante))
                .build();
    }

    private List<ComentarioInternoDTO> mapearComentarios(Aspirante aspirante) {
        if (aspirante.getComentariosInternos() == null) {
            return Collections.emptyList();
        }
        return aspirante.getComentariosInternos().stream()
                .map(c -> ComentarioInternoDTO.builder()
                        .autor(c.getAutor())
                        .texto(c.getTexto())
                        .fecha(c.getFecha())
                        .build())
                .toList();
    }

    public List<AspiranteResponseDTO> toResponseList(List<Aspirante> aspirantes) {
        return aspirantes.stream()
                .map(this::toResponse)
                .toList();
    }
}