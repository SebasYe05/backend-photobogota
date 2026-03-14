package com.photobogota.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.mapper.UsuarioMapper;
import com.photobogota.api.model.Credenciales;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PerfilUsuarioDTO registrar(RegistroRequestDTO dto) {
        
        // Verificar si el email ya está registrado
        if (usuarioRepository.existsByCredenciales_Email(dto.getEmail())) {
            throw new EmailAlreadyExistsException(
                "El email " + dto.getEmail() + " ya está registrado en el sistema"
            );
        }
        
        Miembro miembro = usuarioMapper.toMiembroEntity(dto);
        
        // Inicializar puntos y nivel para nuevos miembros
        miembro.setPuntos(0L);
        miembro.setNivel(1);

        // Crear credenciales con rol de miembro
        Credenciales credenciales = Credenciales.builder()
            .nombreUsuario(dto.getNombreUsuario())
            .email(dto.getEmail())
            .rol(Rol.MIEMBRO)
            .contrasena(passwordEncoder.encode(dto.getContrasena()))
            .build();
        
        miembro.setCredenciales(credenciales);

        Usuario guardado = usuarioRepository.save(miembro);

        return usuarioMapper.toDTO(guardado);
    }
}
