package com.photobogota.api.config;

import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servicio personalizado de Spring Security para cargar los detalles del usuario
 * desde la base de datos MongoDB.
 * 
 * Este servicio implementa UserDetailsService que es usado por JwtAuthenticationFilter
 * para obtener la información del usuario durante la autenticación con JWT.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioAuthRepository usuarioAuthRepository;

    /**
     * Carga los detalles del usuario por su nombre de usuario o email
     * 
     * Este método es llamado por Spring Security cuando se intenta autenticar
     * un usuario con JWT. Busca el usuario en la colección 'usuarios-auth' de MongoDB
     * por email o nombre de usuario.
     * 
     * @param username el nombre de usuario o email a buscar
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar el usuario en la base de datos por email O nombre de usuario
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByEmailOrNombreUsuario(username, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con email o nombre de usuario: " + username));

        // Crear y retornar el objeto UserDetails de Spring Security
        // authorities: los roles/permisos del usuario
        return new User(
                usuarioAuth.getNombreUsuario(), // username (identificador usado en el token JWT)
                usuarioAuth.getContrasena(), // password (ya encriptada con BCrypt)
                // Convertir el rol del usuario a autoridades de Spring Security
                Collections.singletonList(new SimpleGrantedAuthority(
                        "ROLE_" + usuarioAuth.getRol().name()))
        );
    }
}
