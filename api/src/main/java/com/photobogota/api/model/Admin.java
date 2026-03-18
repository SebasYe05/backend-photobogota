package com.photobogota.api.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.TypeAlias;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TypeAlias("admin")
public class Admin extends Usuario {

    @Builder.Default
    private Long accionesRealizadas = 0L;

    private LocalDateTime ultimaConexionPanel;

}
