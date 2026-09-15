package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.LogoutResponseDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioResumenDTO;
import com.photobogota.api.service.IAuthService;

import org.junit.jupiter.api.Test;

class AuthControllerTest extends ControllerTestSupport {

    private final IAuthService authService = mock(IAuthService.class);
    private final AuthController controller = new AuthController(authService);

    @Test
    void registrar_devuelve201() throws Exception {
        when(authService.registrar(any())).thenReturn(mock(RegistroResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/auth/register"),
                        "{\"nombresCompletos\":\"Juan Romero\",\"email\":\"juan@example.com\","
                                + "\"nombreUsuario\":\"juanromero\",\"contrasena\":\"Segura123.\","
                                + "\"fechaNacimiento\":\"1990-05-15\"}"))
                .andExpect(status().isCreated());

        verify(authService).registrar(any());
    }

    @Test
    void registrar_conCuerpoInvalido_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/auth/register"),
                        "{\"nombresCompletos\":\"\",\"email\":\"no-es-email\","
                                + "\"contrasena\":\"123\",\"fechaNacimiento\":\"2030-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void login_devuelve200() throws Exception {
        when(authService.login(any())).thenReturn(mock(LoginResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/auth/login"),
                        "{\"login\":\"juanromero\",\"contrasena\":\"Segura123.\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_devuelve200() throws Exception {
        when(authService.refreshToken(anyString())).thenReturn(mock(LoginResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"abc123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_sinCuerpo_devuelveMensajeLocal() throws Exception {
        mvc(controller)
                .perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada localmente"));
    }

    @Test
    void logout_conRefreshToken_devuelve200() throws Exception {
        when(authService.logout(anyString())).thenReturn(mock(LogoutResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/auth/logout"), "{\"refreshToken\":\"abc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void me_autenticado_devuelve200() throws Exception {
        when(authService.getResumenUsuario("juanromero")).thenReturn(mock(UsuarioResumenDTO.class));

        mvc(controller)
                .perform(get("/api/v1/auth/me").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk());
    }

    @Test
    void me_sinAutenticacion_devuelve401() throws Exception {
        mvc(controller)
                .perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitarRecuperacion_devuelve200ConMensaje() throws Exception {
        when(authService.solicitarRecuperacionContrasena(any())).thenReturn("Correo enviado");

        mvc(controller)
                .perform(json(post("/api/v1/auth/passwords/recovery-request"),
                        "{\"email\":\"juan@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Correo enviado"));
    }

    @Test
    void verificarCodigo_devuelve200ConMensaje() throws Exception {
        when(authService.verificarCodigoYCambiarContrasena(any())).thenReturn("Contraseña actualizada");

        mvc(controller)
                .perform(json(post("/api/v1/auth/passwords/reset"),
                        "{\"email\":\"juan@example.com\",\"codigo\":\"123456\","
                                + "\"nuevaContrasena\":\"Nueva12345.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada"));
    }

    @Test
    void verifySesion_devuelve200ConRol() throws Exception {
        mvc(controller)
                .perform(get("/api/v1/auth/verify-session").with(autenticado("juanromero", "MIEMBRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("juanromero"))
                .andExpect(jsonPath("$.rol").value("MIEMBRO"));
    }
}