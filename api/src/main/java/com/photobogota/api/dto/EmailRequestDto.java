package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO para la solicitud de envío de correo electrónico")
public class EmailRequestDto {

    @Schema(description = "Dirección de correo electrónico del destinatario", example = "recipient@example.com")
    private String to;

    @Schema(description = "Asunto del correo electrónico", example = "Asunto del correo")
    private String subject;
    
    @Schema(description = "Contenido del correo electrónico", example = "Contenido del correo")
    private String content;
}
