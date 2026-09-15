package com.photobogota.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private CredencialesDePrueba credencialesValidas() {
        UserDetails usuario = User.withUsername("foto1")
                .password("x")
                .authorities("ROLE_MIEMBRO")
                .build();
        when(jwtService.extraerNombreUsuario("jwt-123")).thenReturn("foto1");
        when(userDetailsService.loadUserByUsername("foto1")).thenReturn(usuario);
        when(jwtService.esTokenValido("jwt-123", "foto1")).thenReturn(true);
        return new CredencialesDePrueba(usuario);
    }

    @Test
    void sinHeaderAuthorization_noAutenticaYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void headerSinPrefijoBearer_noAutenticaYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spotss");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(204);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenValido_configuraAutenticacionConElUsuarioYContinua() throws Exception {
        CredencialesDePrueba credenciales = credencialesValidas();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer jwt-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isEqualTo(credenciales.usuario());
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_MIEMBRO");
    }

    @Test
    void tokenInvalido_noConfiguraAutenticacion() throws Exception {
        when(jwtService.extraerNombreUsuario("jwt-malo")).thenReturn("foto1");
        when(userDetailsService.loadUserByUsername("foto1"))
                .thenReturn(User.withUsername("foto1").password("x").authorities("ROLE_MIEMBRO").build());
        when(jwtService.esTokenValido("jwt-malo", "foto1")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer jwt-malo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usuarioInexistente_lanzaExcepcionPeroNoBloqueaLasolicitud() throws Exception {
        when(jwtService.extraerNombreUsuario("jwt-fantasma")).thenReturn("noExiste");
        when(userDetailsService.loadUserByUsername("noExiste"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/spots");
        request.addHeader("Authorization", "Bearer jwt-fantasma");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mockChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    private FilterChain mockChain() {
        return org.mockito.Mockito.mock(FilterChain.class);
    }

    private record CredencialesDePrueba(UserDetails usuario) {
    }
}