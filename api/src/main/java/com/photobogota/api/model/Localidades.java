package com.photobogota.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "localidades")
public class Localidades {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private String imagen;

    private Boolean activo = true;
}
