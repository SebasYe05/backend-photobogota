package com.photobogota.api.service; 

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.model.Admin;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Moderador;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Socio;
import com.photobogota.api.model.Usuario;

import java.time.LocalDateTime;

@Component
public class UsuarioFactory {

    public Usuario crearUsuario(ObjectId id, CrearUsuarioRequestDTO dto, Rol rol) {
        switch (rol) {
            case ADMIN:
                return Admin.builder()
                        .id(id)
                        .nombresCompletos(dto.getNombresCompletos())
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .estadoCuenta(true)
                        .correoConfirmado(false)
                        .fechaRegistro(LocalDateTime.now())
                        .ultimaConexionPanel(null)
                        .build();

            case SOCIO:
                return Socio.builder()
                        .id(id)
                        .nombresCompletos(dto.getNombresCompletos())
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .estadoCuenta(true)
                        .correoConfirmado(false)
                        .fechaRegistro(LocalDateTime.now())
                        .build();

            case MOD:
                return Moderador.builder()
                        .id(id)
                        .nombresCompletos(dto.getNombresCompletos())
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .estadoCuenta(true)
                        .correoConfirmado(false)
                        .fechaRegistro(LocalDateTime.now())
                        .build();

            case MIEMBRO:
            default:
                return Miembro.builder()
                        .id(id)
                        .nombresCompletos(dto.getNombresCompletos())
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .estadoCuenta(true)
                        .correoConfirmado(false)
                        .fechaRegistro(LocalDateTime.now())
                        .puntos(0L)
                        .nivel(1)
                        .build();
        }
    }
}