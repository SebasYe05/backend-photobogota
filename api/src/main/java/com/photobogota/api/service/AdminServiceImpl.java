package com.photobogota.api.service;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.RegistroAdminRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.model.Admin;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.AdminRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements IAdminService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    @Override
    @Transactional // Asegura que se guarden AMBOS o NINGUNO
    public RegistroResponseDTO registrarAdmin(RegistroAdminRequestDTO dto) {
        log.info("Intentando registrar admin con email: {}", dto.getEmail());

        // 1. Validaciones previas
        if (usuarioAuthRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }
        
        if (usuarioAuthRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new UsernameAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        // 2. Generar ID compartido
        ObjectId id = new ObjectId();

        // 3. Mapeo de Entidades
        Admin admin = Admin.builder()
                .id(id)
                .nombresCompletos(dto.getNombresCompletos())
                .telefono(dto.getTelefono())
                .biografia(dto.getBiografia())
                .estadoCuenta(true)
                .build();

        UsuarioAuth usuarioAuth = UsuarioAuth.builder()
                .id(id)
                .nombreUsuario(dto.getNombreUsuario())
                .email(dto.getEmail())
                .rol(Rol.ADMIN) // Asignación de rol ADMIN
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .build();

        // 4. Persistencia Atómica
        try {
            adminRepository.save(admin);
            usuarioAuthRepository.save(usuarioAuth);
        } catch (Exception e) {
            log.error("Error crítico al guardar el administrador: {}", e.getMessage());
            throw new RuntimeException("Error al registrar el administrador", e);
        }

        log.info("Admin registrado exitosamente con ID: {}", id);

        return RegistroResponseDTO.builder()
                .fechaRegistro(LocalDateTime.now()) 
                .mensaje("Admin registrado exitosamente")
                .build();
    }
}