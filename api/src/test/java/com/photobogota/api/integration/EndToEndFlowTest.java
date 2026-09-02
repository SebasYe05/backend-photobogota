package com.photobogota.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class EndToEndFlowTest {

    private static final java.util.concurrent.atomic.AtomicInteger IP_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioAuthRepository usuarioAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void limpiar() {
        usuarioAuthRepository.deleteAll();
    }

    private MockHttpServletRequestBuilder conIp(MockHttpServletRequestBuilder builder) {
        return builder.header("X-Test-Client-IP", "10.0.0." + IP_COUNTER.incrementAndGet());
    }

    private String requestBody(String nombreUsuario, String email) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombresCompletos", "Juan Romero");
        body.put("email", email);
        body.put("nombreUsuario", nombreUsuario);
        body.put("contrasena", "Segura123.");
        body.put("fechaNacimiento", LocalDate.of(1990, 5, 15).toString());
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void registrarLoginYConsultarPerfil_FlujoCompleto() throws Exception {
        String usuario = "juan.e2e";
        String email = "juan.e2e@example.com";

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(usuario, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente"));

        MvcResult login = mockMvc.perform(conIp(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + usuario + "\",\"contrasena\":\"Segura123.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("token").asText();

        mockMvc.perform(conIp(get("/api/v1/auth/me"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value(usuario));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        String usuario = "juan.e2e";
        String email = "juan.e2e@example.com";

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(usuario, email)))
                .andExpect(status().isCreated());

        mockMvc.perform(conIp(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + usuario + "\",\"contrasena\":\"Incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoAEscarProtegidoSinTokenDevuelve403() throws Exception {
        mockMvc.perform(conIp(get("/api/v1/auth/me")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrarUsuarioDuplicadoDevuelve409() throws Exception {
        String usuario = "juan.e2e";
        String email = "juan.e2e@example.com";
        String body = requestBody(usuario, email);

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarConFechaFuturaDevuelve400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombresCompletos", "Ana Torres");
        body.put("email", "ana.e2e@example.com");
        body.put("nombreUsuario", "ana.e2e");
        body.put("contrasena", "Segura123.");
        body.put("fechaNacimiento", LocalDate.now().plusYears(1).toString());

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elRolDelUsuarioRegistradoEsMiembro() throws Exception {
        String usuario = "juan.e2e";
        String email = "juan.e2e@example.com";

        mockMvc.perform(conIp(post("/api/v1/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(usuario, email)))
                .andExpect(status().isCreated());

        UsuarioAuth auth = usuarioAuthRepository.findByNombreUsuario(usuario).orElseThrow();

        assert auth.getRol() == Rol.MIEMBRO;
        assert passwordEncoder.matches("Segura123.", auth.getContrasena());
    }
}