package com.photobogota.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;

import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    // Mapeo de Entidad a DTO
    // Si el objeto real es un Miembro, MapStruct usará toDTO(Miembro)
    // automáticamente
    @SubclassMapping(source = Miembro.class, target = PerfilUsuarioDTO.class)
    @Mapping(target = "tipoUsuario", expression = "java(usuario.getClass().getSimpleName().toUpperCase())")
    @Mapping(target = "puntos", ignore = true)
    @Mapping(target = "nivel", ignore = true)
    @Mapping(target = "email", source = "credenciales.email")
    @Mapping(target = "nombreUsuario", source = "credenciales.nombreUsuario")
    PerfilUsuarioDTO toDTO(Usuario usuario);

    // Mapeo específico para Miembro (para que no pierda puntos ni nivel)
    @Mapping(target = "tipoUsuario", constant = "miembro")
    @Mapping(target = "email", source = "credenciales.email")
    @Mapping(target = "nombreUsuario", source = "credenciales.nombreUsuario")
    PerfilUsuarioDTO toDTO(Miembro miembro);

    // Mapeo de Registro a Entidad
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "fotoPefil", ignore = true)
    @Mapping(target = "biografia", ignore = true)
    @Mapping(target = "estadoCuenta", constant = "true")
    @Mapping(target = "telefono", ignore = true)
    @Mapping(target = "puntos", ignore = true)
    @Mapping(target = "nivel", ignore = true)
    @Mapping(target = "credenciales", ignore = true)
    Miembro toMiembroEntity(RegistroRequestDTO dto);

    // Método genérico que delega a toMiembroEntity (usado para registro de nuevos
    // usuarios)
    default Usuario toEntity(RegistroRequestDTO dto) {
        return toMiembroEntity(dto);
    }
}