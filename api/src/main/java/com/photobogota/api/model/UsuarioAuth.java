package com.photobogota.api.model;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase que contiene las credenciales de autenticación del usuario.
 * Se guarda en la colección "usuarios-auth" con su propio ID que debe coincidir con el del perfil.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "usuarios-auth")
public class UsuarioAuth {
    private ObjectId id;
    private String nombreUsuario;
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;
    private Rol rol;
}
