package com.photobogota.api.service;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.mapper.UsuarioMapper;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RegistroResponseDTO registrar(RegistroRequestDTO dto) {
        
        // Verificar si el email ya está registrado en usuarios-auth
        if (usuarioAuthRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException(
                "El email " + dto.getEmail() + " ya está registrado en el sistema"
            );
        }
        
        // Generar el ID que se usará en ambas colecciones
        ObjectId id = new ObjectId();
        
        // Crear el perfil del usuario (colección: usuarios)
        Miembro miembro = usuarioMapper.toMiembroEntity(dto);
        miembro.setId(id);
        
        // Inicializar puntos y nivel para nuevos miembros
        miembro.setPuntos(0L);
        miembro.setNivel(1);

        // Crear las credenciales (colección: usuarios-auth) con el mismo ID
        UsuarioAuth usuarioAuth = UsuarioAuth.builder()
            .id(id)
            .nombreUsuario(dto.getNombreUsuario())
            .email(dto.getEmail())
            .rol(Rol.MIEMBRO)
            .contrasena(passwordEncoder.encode(dto.getContrasena()))
            .build();
        
        // Guardar en ambas colecciones con el mismo ID
        Usuario usuarioGuardado = usuarioRepository.save(miembro);
        usuarioAuthRepository.save(usuarioAuth);

        // Retornar solo la fecha de registro
        return RegistroResponseDTO.builder()
            .fechaRegistro(usuarioGuardado.getFechaRegistro())
            .mensaje("Usuario registrado exitosamente")
            .build();
    }
}
