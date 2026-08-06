package com.photobogota.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "registros_moderacion")
public class RegistroModeracion {

    @Id
    private String id;

    @Indexed
    private String usuarioId;

    private String nombreUsuario;

    private AccionModeracion accion;

    private TipoContenidoModerado tipoContenido;

    private String contenidoOriginal;

    @Builder.Default
    private List<String> palabrasDetectadas = new ArrayList<>();

    private String detalle;

    private String origen;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    private EstadoApelacion estadoApelacion;

    private String motivoApelacion;

    private LocalDateTime fechaApelacion;

    private String respuestaApelacion;

    private String revisadaPor;

    private LocalDateTime fechaRevision;
}
