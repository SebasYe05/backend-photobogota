package com.photobogota.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Información sobre un archivo de log")
public class LogFileInfo {
    @Schema(description = "Nombre del archivo de log", example = "app.log")
    private String name;

    @Schema(description = "Tamaño del archivo de log", example = "1024")
    private long size;

    @Schema(description = "Timestamp de la última modificación (epoch ms)", example = "1714500000000")
    private long lastModified;
}