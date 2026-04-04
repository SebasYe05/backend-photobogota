package com.photobogota.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResenaRequestDTO {

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @NotBlank(message = "El comentario es obligatorio")
    private String comentario;
}