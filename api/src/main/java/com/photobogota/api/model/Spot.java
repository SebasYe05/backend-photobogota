package com.photobogota.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "spots")
public class Spot {

    @Id
    private String id;

    private String nombre;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String categoria;
    private String localidad;
    private String descripcion;
    private String recomendacion;
    private String tipsFoto;

    private List<String> imagenes = new ArrayList<>(); // URLs en tu storage

    private Double rating = 0.0;
    private Integer totalResenas = 0;

    private String creadorUsername; // nombreUsuario del creador (SOCIO/MODERADOR)

    private List<Resena> resenas = new ArrayList<>();

    private LocalDateTime creadoEn = LocalDateTime.now();

    @Data
    public static class Resena {
        private String id;
        private String usuario;      // nombreUsuario
        private String avatar;
        private Integer rating;
        private String comentario;
        private LocalDateTime fecha = LocalDateTime.now();
    }
}