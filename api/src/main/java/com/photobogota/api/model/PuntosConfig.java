package com.photobogota.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "puntos_config")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PuntosConfig {

    @Id
    private String clave;

    private String valor;
}
