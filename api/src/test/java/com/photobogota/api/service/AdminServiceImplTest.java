package com.photobogota.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioFactory usuarioFactory;

    @InjectMocks
    private AdminServiceImpl adminService;

    private CrearUsuarioRequestDTO dtoDeEjemplo() {
        CrearUsuarioRequestDTO dto = new CrearUsuarioRequestDTO();
        dto.setNombresCompletos("Juan Pérez");
        dto.setEmail("juan@photobogota.com");
        dto.setNombreUsuario("juanperez");
        dto.setContrasena("Segura123.");
        dto.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        dto.setRol("MIEMBRO");
        return dto;
    }

    @Test
    void crearUsuario_datosValidos_registraEnAmbasColecciones() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(false);
        Miembro miembro = Miembro.builder().nombresCompletos("Juan Pérez").build();
        when(usuarioFactory.crearUsuario(any(ObjectId.class), any(CrearUsuarioRequestDTO.class), eq(Rol.MIEMBRO)))
                .thenReturn(miembro);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioAuthRepository.save(any(UsuarioAuth.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("Segura123.")).thenReturn("hash");

        RegistroResponseDTO respuesta = adminService.crearUsuario(dtoDeEjemplo());

        assertThat(respuesta.getMensaje()).isEqualTo("Usuario registrado exitosamente");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(usuarioAuthRepository).save(any(UsuarioAuth.class));
    }

    @Test
    void crearUsuario_emailYaRegistrado_lanzaExcepcionYNoGuarda() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.crearUsuario(dtoDeEjemplo()))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("email ya está registrado");
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioAuthRepository, never()).save(any(UsuarioAuth.class));
    }

    @Test
    void crearUsuario_nombreDeUsuarioYaEnUso_lanzaExcepcion() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> adminService.crearUsuario(dtoDeEjemplo()))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void crearUsuario_rolInvalido_seAsumeMiembro() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(false);
        Miembro miembro = Miembro.builder().nombresCompletos("Juan Pérez").build();
        when(usuarioFactory.crearUsuario(any(ObjectId.class), any(CrearUsuarioRequestDTO.class), eq(Rol.MIEMBRO)))
                .thenReturn(miembro);

        CrearUsuarioRequestDTO dto = dtoDeEjemplo();
        dto.setRol("ROL_INEXISTENTE");

        adminService.crearUsuario(dto);

        verify(usuarioFactory).crearUsuario(any(ObjectId.class), any(CrearUsuarioRequestDTO.class), eq(Rol.MIEMBRO));
    }

    @Test
    void listarUsuarios_conAuth_combinaPerfilYCredenciales() {
        ObjectId id = new ObjectId();
        Pageable pageable = PageRequest.of(0, 10);
        Usuario usuario = Miembro.builder()
                .id(id)
                .nombresCompletos("Juan Pérez")
                .fechaRegistro(java.time.LocalDateTime.now())
                .estadoCuenta(true)
                .build();
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuario), pageable, 1));
        when(usuarioAuthRepository.findById(id)).thenReturn(Optional.of(UsuarioAuth.builder()
                .id(id)
                .nombreUsuario("juanperez")
                .email("juan@photobogota.com")
                .rol(Rol.MIEMBRO)
                .build()));

        Page<UsuarioListResponseDTO> resultado = adminService.listarUsuarios(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombreUsuario()).isEqualTo("juanperez");
        assertThat(resultado.getContent().get(0).getRol()).isEqualTo("MIEMBRO");
        assertThat(resultado.getContent().get(0).getNombresCompletos()).isEqualTo("Juan Pérez");
    }

    @Test
    void listarUsuarios_sinAuth_seSaltaElPerfilSinCredenciales() {
        ObjectId id = new ObjectId();
        Pageable pageable = PageRequest.of(0, 10);
        Usuario usuario = Miembro.builder().id(id).nombresCompletos("Juan Pérez").build();
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuario), pageable, 1));
        when(usuarioAuthRepository.findById(id)).thenReturn(Optional.empty());

        Page<UsuarioListResponseDTO> resultado = adminService.listarUsuarios(pageable);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    void actualizarEstadoUsuario_actualizaElEstadoDelPerfil() {
        ObjectId id = new ObjectId();
        Usuario usuario = Miembro.builder().id(id).estadoCuenta(true).build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.actualizarEstadoUsuario(id.toHexString(), false);

        assertThat(usuario.getEstadoCuenta()).isFalse();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void eliminarUsuario_eliminaDeAmbasColecciones() {
        ObjectId id = new ObjectId();

        adminService.eliminarUsuario(id.toHexString());

        verify(usuarioRepository).deleteById(id);
        verify(usuarioAuthRepository).deleteById(id);
    }
}