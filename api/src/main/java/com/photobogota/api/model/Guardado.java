package com.photobogota.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "guardados")
public class Guardado {

    @Id
    private String id;

    private String usuario;

    private String spotId;

    @Builder.Default
    private LocalDateTime guardadoEn = LocalDateTime.now();
}