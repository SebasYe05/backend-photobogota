package com.photobogota.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "palabras_prohibidas")
public class PalabraProhibida {

    @Id
    private String id;

    private String texto;

    private TipoPalabra tipo;

    @Builder.Default
    private Boolean activo = true;

    private String categoria;

    private List<String> excepciones = new ArrayList<>();

    private String creadoPor;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
