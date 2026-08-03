package com.photobogota.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aspirantes_socios")
public class Aspirante {

    @Id
    private String id;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private String direccion;
    private String nit;
    private LocalDate fechaNacimiento;
    private String nombrePropietario;
    private String razonSocial;
    private String categoria;
    private String localidad;
    private String rutaArchivo;
    private String tipoArchivo;
    private EstadoAspirante estado;
    private LocalDate fechaSolicitud;
    private String codigo;

    // Justificación dejada por el moderador al rechazar o al pedir correcciones.
    private String motivoDecision;

    // nombreUsuario/email del moderador que tomó la última decisión (aprobar,
    // rechazar o pedir correcciones), para auditoría.
    private String decididoPor;

    // Fecha y hora en la que se tomó la última decisión.
    private LocalDateTime fechaDecision;

    // Fecha en la que el aspirante reenvió sus documentos tras una corrección.
    private LocalDateTime fechaReenvio;

    // Cuántas veces el aspirante ha reenviado su solicitud tras una corrección.
    @Builder.Default
    private Integer vecesCorregida = 0;

    // Bitácora de comentarios internos entre moderadores/administradores,
    // no visibles para el aspirante. Sirve para coordinar la revisión.
    @Builder.Default
    private List<ComentarioInterno> comentariosInternos = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComentarioInterno {
        private String autor;
        private String texto;
        private LocalDateTime fecha;
    }

}
