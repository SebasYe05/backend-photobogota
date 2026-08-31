package com.photobogota.api.config;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private UsuarioAuth usuarioAuthDeEjemplo() {
        return UsuarioAuth.builder()
                .id(new ObjectId())
                .nombreUsuario("juan")
                .email("juan@example.com")
                .contrasena("$2a$10$hash")
                .rol(Rol.MIEMBRO)
                .build();
    }

    @Test
    void loadUserByUsername_cargaLasCredencialesYElRol() {
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("juan"), eq("juan")))
                .thenReturn(Optional.of(usuarioAuthDeEjemplo()));

        UserDetails detalles = customUserDetailsService.loadUserByUsername("juan");

        assertThat(detalles.getUsername()).isEqualTo("juan");
        assertThat(detalles.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(detalles.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MIEMBRO");
        verify(usuarioAuthRepository).findByEmailOrNombreUsuario("juan", "juan");
    }

    @Test
    void loadUserByUsername_buscaTambienPorEmail() {
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("juan@example.com"), eq("juan@example.com")))
                .thenReturn(Optional.of(usuarioAuthDeEjemplo()));

        UserDetails detalles = customUserDetailsService.loadUserByUsername("juan@example.com");

        assertThat(detalles.getUsername()).isEqualTo("juan");
    }

    @Test
    void loadUserByUsername_usuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("fantasma"), eq("fantasma")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma");
    }
}