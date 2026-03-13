package com.photobogota.api.model;

import org.springframework.data.annotation.TypeAlias;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true) // Esto es importante para que los campos de Usuario se consideren en equals() y hashCode()
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("miembro") // Esto evita que Mongo guarde el nombre completo del paquete en la DB
public class Miembro extends Usuario {
    private Long puntos;
	private Integer nivel;

}
