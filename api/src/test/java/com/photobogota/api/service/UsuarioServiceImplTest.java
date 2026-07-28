package com.photobogota.api.service;

import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.GuardadoRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private GuardadoRepository guardadoRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void editarPerfil_CuandoUsuarioExiste_DebeActualizarYRetornarPerfilDTO() {

        String nombreUsuario = "bogotano123";
        ObjectId userId = new ObjectId();

        EditarPerfilDTO dtoEdicion = new EditarPerfilDTO();
        dtoEdicion.setNombresCompletos("Juan Pérez Editado");
        dtoEdicion.setBiografia("Nueva biografía sobre Bogotá");
        dtoEdicion.setFotoPerfil("http://imagenes.com/nueva.jpg");

        UsuarioAuth usuarioAuthSimulado = new UsuarioAuth();
        usuarioAuthSimulado.setId(userId);
        usuarioAuthSimulado.setNombreUsuario(nombreUsuario);
        usuarioAuthSimulado.setEmail("juan@photobogota.com");
        usuarioAuthSimulado.setRol(Rol.MIEMBRO);

        Miembro miembroSimulado = Miembro.builder()
                        .id(userId)
                        .nombresCompletos("Juan Pérez Original")
                        .biografia("Biografía vieja")
                        .fotoPerfil("http://imagenes.com/vieja.jpg")
                        .puntos(100L)
                        .nivel(1)
                        .build();

        when(usuarioAuthRepository.findByNombreUsuario(nombreUsuario))
                        .thenReturn(Optional.of(usuarioAuthSimulado));

        when(usuarioRepository.findById(usuarioAuthSimulado.getId()))
                        .thenReturn(Optional.of(miembroSimulado));

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(spotRepository.countByCreadorUsername(nombreUsuario)).thenReturn(0L);
        when(spotRepository.findByResenasUsuario(nombreUsuario)).thenReturn(Collections.emptyList());
        when(guardadoRepository.countByNombreUsuario(nombreUsuario)).thenReturn(0L);

        PerfilUsuarioDTO resultado = usuarioService.editarPerfil(nombreUsuario, dtoEdicion);

        assertThat(resultado).isNotNull();

        assertThat(resultado.getNombresCompletos()).isEqualTo("Juan Pérez Editado");
        assertThat(resultado.getBiografia()).isEqualTo("Nueva biografía sobre Bogotá");
        assertThat(resultado.getFotoPerfil()).isEqualTo("http://imagenes.com/nueva.jpg");

        assertThat(resultado.getNombreUsuario()).isEqualTo(nombreUsuario);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void editarPerfil_CuandoUsuarioNoExiste_DebeLanzarExcepcion() {

        String nombreUsuarioInexistente = "usuario_fantasma";
        EditarPerfilDTO dto = new EditarPerfilDTO();

        when(usuarioAuthRepository.findByNombreUsuario(nombreUsuarioInexistente))
                        .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.editarPerfil(nombreUsuarioInexistente, dto))
                        .isInstanceOf(RuntimeException.class);

        verify(usuarioRepository, never()).findById(any());
        verify(usuarioRepository, never()).save(any());
    }
}