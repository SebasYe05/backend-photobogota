package com.photobogota.api.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String token;

    private String email; // referencia al usuario

    private Date fechaExpiracion;

    private boolean revocado;

    private Date fechaCreacion;
}