package com.photobogota.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobogota.api.model.Admin;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.RefreshTokenRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.Map;

/**
 * Matriz de autorización y filtros de seguridad probada contra el contexto
 * real (filtros JWT, rate-limit y mantenimiento incluidos) con MongoDB local.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioAuthRepository usuarioAuthRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @BeforeEach
    void limpiarBase() {
        sesionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        usuarioAuthRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String registrarMiembro(String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Test-Client-IP", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombresCompletos", "Fotografo Uno",
                                "email", "foto1@correo.com",
                                "nombreUsuario", "foto1",
                                "contrasena", "Contrasena123",
                                "fechaNacimiento", "1995-05-15"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String loginYToken(String ip) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Test-Client-IP", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"foto1\",\"contrasena\":\"Contrasena123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private String tokenAdmin(String ip) {
        Admin admin = Admin.builder()
                .nombresCompletos("Admin Uno")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
        usuarioRepository.save(admin);
        usuarioAuthRepository.save(UsuarioAuth.builder()
                .nombreUsuario("adminUno")
                .email("admin@correo.com")
                .contrasena("encriptada")
                .rol(Rol.ADMIN)
                .build());
        return jwtService.generarToken("adminUno");
    }

    @Test
    void rutasPublicas_accesiblesSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/spots")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/localidades")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/mantenimiento/estado")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void rutasProtegidas_sinToken_seBloqueanCon403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notificaciones")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    void loginConCredencialesIncorrectas_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Test-Client-IP", "10.0.0.21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"nadie\",\"contrasena\":\"Incorrecta123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void miembro_accedeSusRutasPeroNoALasDeModeradorNiAdmin() throws Exception {
        registrarMiembro("10.0.0.31");
        String token = loginYToken("10.0.0.31");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("foto1"));

        mockMvc.perform(post("/api/v1/spots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Parque Central\",\"latitud\":4.6,\"longitud\":-74.08,"
                                + "\"direccion\":\"Calle 1\",\"categoria\":\"Parque\",\"localidad\":\"Kennedy\","
                                + "\"descripcion\":\"Un gran parque para fotos\",\"tipo\":\"SPOT\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/moderador/categorias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_accedeALasRutasDeAdministracion() throws Exception {
        String token = tokenAdmin("10.0.0.41");

        mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/rate-limit/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIPs").isNumber());
    }

    @Test
    void adminNoAccedeALaRutasExclusivasDelModerador() throws Exception {
        String token = tokenAdmin("10.0.0.42");

        mockMvc.perform(get("/api/v1/moderador/categorias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenInvalidoDevuelve403EnRutasProtegidas() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token-invalido-abc"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_preflightPermitidoParaOrigenConfigurado() throws Exception {
        mockMvc.perform(options("/api/v1/spots")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/spots")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}