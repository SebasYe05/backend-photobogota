package com.photobogota.api.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.dto.ReenvioDocumentosDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.exception.AspiranteAlreadyExistsException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.AspiranteMapper;
import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Socio;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.AspiranteRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AspiranteServiceImpl implements IAspiranteService {

    // Días que debe esperar un aspirante rechazado antes de poder
    // volver a aplicar (criterio de aceptación #3 de la HU de notificaciones).
    private static final int DIAS_ESPERA_REAPLICACION = 90;

    private static final DateTimeFormatter FORMATO_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AspiranteRepository aspiranteRepository;
    private final AspiranteMapper aspiranteMapper;
    private final IEmailService emailService;
    private final INotificacionService notificacionService;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioFactory usuarioFactory;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.soporte.email:photobogota123@gmail.com}")
    private String soporteEmail;

    @Value("${app.manual.socio.url:https://photobogota.com/manual-socio}")
    private String manualSocioUrl;

    @Override
    public AspiranteResponseDTO crearSolicitud(SolicitudAspiranteDTO request) {
        validarPuedeAplicar(request.getEmail(), request.getNit());

        String codigoGenerado = generarCodigo();

        Aspirante aspirante = mapearSolicitud(request, codigoGenerado);
        Aspirante savedAspirante = aspiranteRepository.save(aspirante);

        String htmlContent = construirHtmlSolicitud(
            request.getNombres(),
            request.getApellidos(),
            codigoGenerado
        );

        enviarCorreoSeguro(request.getEmail(), "Tu código de solicitud - PhotoBogota", htmlContent);

        log.info("Solicitud creada para {} con código {}", request.getEmail(), codigoGenerado);

        return aspiranteMapper.toResponse(savedAspirante);
    }

    // Un aspirante puede tener, con el tiempo, varios documentos de solicitud
    // (si fue rechazado y reaplica). Aquí validamos que:
    //   1) No tenga ya una solicitud activa o aprobada (con este email o NIT).
    //   2) Si su última solicitud fue rechazada, ya pasaron los 90 días.
    private void validarPuedeAplicar(String email, String nit) {
        List<Aspirante> previas = new ArrayList<>();
        previas.addAll(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc(email));
        previas.addAll(aspiranteRepository.findByNitOrderByFechaSolicitudDesc(nit));

        for (Aspirante previa : previas) {
            if (previa.getEstado() != EstadoAspirante.RECHAZADO) {
                throw new AspiranteAlreadyExistsException(
                        "Ya existe una solicitud en trámite o aprobada con este email o NIT/RUT");
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (Aspirante previa : previas) {
            if (previa.getFechaDecision() == null) {
                continue;
            }
            LocalDateTime puedeReaplicarDesde = previa.getFechaDecision().plusDays(DIAS_ESPERA_REAPLICACION);
            if (ahora.isBefore(puedeReaplicarDesde)) {
                throw new OperacionInvalidaException(
                        "Tu solicitud anterior fue rechazada. Podrás volver a aplicar a partir del "
                                + puedeReaplicarDesde.format(FORMATO_FECHA_CORTA));
            }
        }
    }

    private Aspirante mapearSolicitud(SolicitudAspiranteDTO request, String codigo) {
        Aspirante aspirante = new Aspirante();
        aspirante.setNombres(request.getNombres());
        aspirante.setApellidos(request.getApellidos());
        aspirante.setEmail(request.getEmail());
        aspirante.setTelefono(request.getTelefono());
        aspirante.setDireccion(request.getDireccion());
        aspirante.setNit(request.getNit());
        aspirante.setFechaNacimiento(request.getFechaNacimiento());
        aspirante.setNombrePropietario(request.getNombrePropietario());
        aspirante.setRazonSocial(request.getRazonSocial());
        aspirante.setCategoria(request.getCategoria());
        aspirante.setLocalidad(request.getLocalidad());
        aspirante.setRutaArchivo(request.getRutaArchivo());
        aspirante.setTipoArchivo(request.getTipoArchivo());
        aspirante.setEstado(EstadoAspirante.PENDIENTE);
        aspirante.setFechaSolicitud(LocalDate.now());
        aspirante.setCodigo(codigo);
        aspirante.setVecesCorregida(0);
        return aspirante;
    }

    private String generarCodigo() {
        String prefijo = "SOL-";
        String numeros = String.format("%06d", new java.util.Random().nextInt(999999));
        return prefijo + numeros;
    }

    // ==================== PLANTILLAS DE CORREO ====================
    // Todas incluyen el número de ticket (el código de seguimiento de la
    // solicitud), tal como pide el criterio de aceptación #5.

    private String construirHtmlSolicitud(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "¡Hola, " + nombres + " " + apellidos + "!",
            "Tu solicitud para convertirte en socio de PhotoBogota ha sido registrada exitosamente.",
            codigo,
            "Guarda este código (tu número de ticket) para dar seguimiento a tu solicitud. Nuestro equipo la revisará y te contactaremos pronto."
        );
    }

    private String construirHtmlCorreccion(String nombres, String apellidos, String codigo, String motivo) {
        String enlaceFormulario = frontendUrl + "/solicitud-enviada";
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Un moderador de PhotoBogota revisó tu solicitud (ticket " + codigo
                + ") y necesita que corrijas lo siguiente antes de continuar.",
            codigo,
            "<strong>Cambios requeridos:</strong> " + motivo
                + "<br/><br/>Ingresa a <a href=\"" + enlaceFormulario + "\">" + enlaceFormulario
                + "</a>, escribe tu código de solicitud y sube la documentación corregida."
        );
    }

    private String construirHtmlRechazo(String nombres, String apellidos, String codigo, String motivo) {
        String puedeReaplicarDesde = LocalDateTime.now().plusDays(DIAS_ESPERA_REAPLICACION).format(FORMATO_FECHA_CORTA);
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Lamentamos informarte que tu solicitud (ticket " + codigo + ") para ser socio de PhotoBogota fue rechazada.",
            codigo,
            "<strong>Motivo:</strong> " + motivo
                + "<br/><br/>Podrás enviar una nueva solicitud a partir del <strong>" + puedeReaplicarDesde + "</strong> (90 días después de esta decisión)."
        );
    }

    private String construirHtmlAprobado(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "¡Felicidades, " + nombres + " " + apellidos + "!",
            "Tu solicitud (ticket " + codigo + ") para ser socio de PhotoBogota fue aprobada. Estamos preparando la creación de tu cuenta.",
            codigo,
            "En los próximos días recibirás un correo aparte con tus credenciales de acceso a la plataforma."
        );
    }

    private String construirHtmlCredenciales(String nombres, String apellidos, String codigo,
            String nombreUsuario, String passwordTemporal) {
        String infoAdicional = "<strong>Usuario:</strong> " + nombreUsuario
                + "<br/><strong>Contraseña temporal:</strong> " + passwordTemporal
                + "<br/><em>Te recomendamos cambiarla apenas inicies sesión.</em>"
                + "<br/><br/><strong>Manual del socio:</strong> <a href=\"" + manualSocioUrl + "\">" + manualSocioUrl + "</a>"
                + "<br/><strong>Contacto de soporte:</strong> " + soporteEmail
                + "<br/><br/>Número de ticket de tu solicitud: <strong>" + codigo + "</strong>";

        return construirPlantillaBase(
            "¡Bienvenido a PhotoBogota, " + nombres + " " + apellidos + "!",
            "Ya eres socio de PhotoBogota. Aquí tienes tus credenciales de acceso a la plataforma.",
            codigo,
            infoAdicional
        );
    }

    // Caso "ascenso": el aspirante ya tenía cuenta como MIEMBRO con este email.
    // No se generan credenciales nuevas porque su usuario y contraseña ya
    // existían; solo se le informa que su cuenta ya tiene el rol de socio.
    private String construirHtmlAscensoSocio(String nombres, String apellidos, String codigo, String nombreUsuario) {
        String infoAdicional = "Tu cuenta (<strong>" + nombreUsuario + "</strong>) ahora tiene el rol de <strong>socio</strong>."
                + "<br/><em>Usa tu usuario y contraseña habituales para iniciar sesión; no necesitas crear una cuenta nueva.</em>"
                + "<br/><br/><strong>Manual del socio:</strong> <a href=\"" + manualSocioUrl + "\">" + manualSocioUrl + "</a>"
                + "<br/><strong>Contacto de soporte:</strong> " + soporteEmail
                + "<br/><br/>Número de ticket de tu solicitud: <strong>" + codigo + "</strong>";

        return construirPlantillaBase(
            "¡Felicidades, " + nombres + " " + apellidos + "!",
            "Tu solicitud (ticket " + codigo + ") para ser socio de PhotoBogota fue aprobada y tu cuenta ya fue ascendida.",
            codigo,
            infoAdicional
        );
    }

    private String construirHtmlReenvioConfirmado(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Recibimos los documentos que reenviaste para tu solicitud (ticket " + codigo + ") de ingreso a PhotoBogota.",
            codigo,
            "Tu solicitud vuelve a estar en revisión. Te notificaremos por correo en cuanto sea evaluada nuevamente."
        );
    }

    private String construirPlantillaBase(String titulo, String parrafo, String codigo, String infoAdicional) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Solicitud de Socio - PhotoBogota</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f4f4f9;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: #9b8dcf;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 { color: #ffffff; font-size: 28px; margin-bottom: 5px; }
                    .header p { color: #e0e0e0; font-size: 14px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .content h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }
                    .content p { color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 25px; }
                    .code-box {
                        background-color: #f8f9fa;
                        border: 2px dashed #667eea;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 25px 0;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #667eea;
                        letter-spacing: 5px;
                    }
                    .info-box {
                        background-color: #e7f3ff;
                        border-left: 4px solid #2196F3;
                        padding: 15px;
                        margin: 25px 0;
                        text-align: left;
                        border-radius: 4px;
                    }
                    .info-box p { color: #333333; font-size: 14px; margin: 0; }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p { color: #999999; font-size: 12px; margin: 5px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>PhotoBogota</h1>
                        <p>Solicitud de Socio</p>
                    </div>
                    <div class="content">
                        <h2>%TITULO%</h2>
                        <p>%PARRAFO%</p>
                        <div class="code-box">
                            <span class="code">%CODIGO%</span>
                        </div>
                        <div class="info-box">
                            <p>%INFO_ADICIONAL%</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 PhotoBogota. Todos los derechos reservados.</p>
                        <p>Este correo fue enviado automáticamente.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("%TITULO%", titulo)
            .replace("%PARRAFO%", parrafo)
            .replace("%CODIGO%", codigo)
            .replace("%INFO_ADICIONAL%", infoAdicional);
    }

    @Override
    public AspiranteResponseDTO obtenerPorId(String id) {
        Aspirante aspirante = buscarPorId(id);
        return aspiranteMapper.toResponse(aspirante);
    }

    @Override
    public AspiranteResponseDTO obtenerPorEmail(String email) {
        // Con reaplicaciones, puede haber varios documentos por email; nos
        // interesa el más reciente (ya viene ordenado desc por fecha).
        Aspirante aspirante = aspiranteRepository.findByEmailOrderByFechaSolicitudDesc(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Aspirante no encontrado con email: " + email));
        return aspiranteMapper.toResponse(aspirante);
    }

    @Override
    public AspiranteResponseDTO obtenerPorCodigo(String codigo) {
        Aspirante aspirante = buscarPorCodigo(codigo);
        return aspiranteMapper.toResponse(aspirante);
    }

    @Override
    public List<AspiranteResponseDTO> obtenerTodos() {
        return aspiranteMapper.toResponseList(aspiranteRepository.findAll());
    }

    @Override
    public List<AspiranteResponseDTO> obtenerPorEstado(EstadoAspirante estado) {
        return aspiranteMapper.toResponseList(aspiranteRepository.findByEstado(estado));
    }

    @Override
    public AspiranteResponseDTO aprobarAspirante(String id, String responsable) {
        Aspirante aspirante = buscarPorId(id);
        validarTransicionDesdeRevision(aspirante.getEstado());

        aspirante.setEstado(EstadoAspirante.ENVIO_CREDENCIALES);
        registrarDecision(aspirante, null, responsable);
        Aspirante guardado = aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "Tu solicitud fue aprobada - PhotoBogota",
                construirHtmlAprobado(aspirante.getNombres(), aspirante.getApellidos(), aspirante.getCodigo()));

        log.info("Aspirante {} aprobado por {}. Queda en espera de envío de credenciales", id, responsable);
        return aspiranteMapper.toResponse(guardado);
    }

    @Override
    public AspiranteResponseDTO rechazarAspirante(String id, String motivo, String responsable) {
        Aspirante aspirante = buscarPorId(id);
        validarTransicionDesdeRevision(aspirante.getEstado());

        aspirante.setEstado(EstadoAspirante.RECHAZADO);
        registrarDecision(aspirante, motivo, responsable);
        Aspirante guardado = aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "Tu solicitud fue rechazada - PhotoBogota",
                construirHtmlRechazo(aspirante.getNombres(), aspirante.getApellidos(), aspirante.getCodigo(), motivo));

        log.info("Aspirante {} rechazado por {}. Motivo: {}", id, responsable, motivo);
        return aspiranteMapper.toResponse(guardado);
    }

    @Override
    public AspiranteResponseDTO solicitarCorreccion(String id, String motivo, String responsable) {
        Aspirante aspirante = buscarPorId(id);
        validarTransicionDesdeRevision(aspirante.getEstado());

        aspirante.setEstado(EstadoAspirante.EN_CORRECCION);
        registrarDecision(aspirante, motivo, responsable);
        Aspirante guardado = aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "Tu solicitud necesita correcciones - PhotoBogota",
                construirHtmlCorreccion(aspirante.getNombres(), aspirante.getApellidos(), aspirante.getCodigo(), motivo));

        log.info("Aspirante {} devuelto para corrección por {}. Motivo: {}", id, responsable, motivo);
        return aspiranteMapper.toResponse(guardado);
    }

    // Este es el "módulo de credenciales": deja la solicitud en estado
    // APROBADO (proceso terminado), y según el caso:
    //   - Si el email ya pertenece a una cuenta MIEMBRO existente, la
    //     asciende a SOCIO (conservando usuario/contraseña actuales).
    //   - Si no existe ninguna cuenta con ese email, crea una nueva cuenta
    //     de Usuario con rol SOCIO y una contraseña temporal.
    //   - Si el email ya pertenece a una cuenta SOCIO/MOD/ADMIN, sí es un
    //     conflicto real y se bloquea para revisión manual.
    @Override
    @Transactional
    public AspiranteResponseDTO enviarCredenciales(String id, String responsable) {
        Aspirante aspirante = buscarPorId(id);

        if (aspirante.getEstado() != EstadoAspirante.ENVIO_CREDENCIALES) {
            throw new OperacionInvalidaException(
                    "Solo se pueden enviar credenciales a solicitudes aprobadas en espera de envío de credenciales (estado actual: "
                            + aspirante.getEstado() + ")");
        }

        if (aspirante.getUsuarioCreadoId() != null) {
            throw new OperacionInvalidaException("Ya se enviaron las credenciales para esta solicitud");
        }

        Optional<UsuarioAuth> authExistente = usuarioAuthRepository.findByEmail(aspirante.getEmail());

        if (authExistente.isPresent()) {
            promoverCuentaExistenteASocio(aspirante, authExistente.get(), responsable);
        } else {
            crearCuentaSocioDesdeCero(aspirante, responsable);
        }

        Aspirante guardado = buscarPorId(id);
        return aspiranteMapper.toResponse(guardado);
    }

    // Caso más frecuente en la práctica: la persona ya estaba registrada
    // como MIEMBRO (con su propio usuario/contraseña) antes de aplicar a
    // ser socio, usando el mismo correo. En ese caso NO se debe bloquear ni
    // crear una cuenta nueva: se asciende la cuenta existente a SOCIO,
    // conservando su usuario y contraseña de siempre.
    // Solo si el correo ya pertenece a una cuenta SOCIO/MOD/ADMIN es un
    // conflicto real (ej. solicitud duplicada) que sí requiere revisión manual.
    private void promoverCuentaExistenteASocio(Aspirante aspirante, UsuarioAuth authExistente, String responsable) {
        if (authExistente.getRol() != Rol.MIEMBRO) {
            throw new OperacionInvalidaException(
                    "Ya existe una cuenta de usuario con este correo y rol " + authExistente.getRol()
                            + ". Revisa manualmente antes de continuar");
        }

        ObjectId usuarioId = authExistente.getId();
        Usuario usuarioBase = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el perfil de usuario asociado a " + aspirante.getEmail()));

        // Se reconstruye el mismo usuario pero como Socio (mismo _id), para
        // que Spring Data actualice el discriminador de tipo en Mongo sin
        // perder sus datos base ni tener que tocar la contraseña.
        Socio socioAscendido = Socio.builder()
                .id(usuarioId)
                .nombresCompletos(usuarioBase.getNombresCompletos())
                .fechaNacimiento(usuarioBase.getFechaNacimiento())
                .telefono(usuarioBase.getTelefono())
                .fotoPerfil(usuarioBase.getFotoPerfil())
                .estadoCuenta(usuarioBase.getEstadoCuenta())
                .biografia(usuarioBase.getBiografia())
                .correoConfirmado(usuarioBase.getCorreoConfirmado())
                .fechaRegistro(usuarioBase.getFechaRegistro())
                .enMora(false)
                .build();
        usuarioRepository.save(socioAscendido);

        authExistente.setRol(Rol.SOCIO);
        usuarioAuthRepository.save(authExistente);

        aspirante.setEstado(EstadoAspirante.APROBADO);
        aspirante.setUsuarioCreadoId(usuarioId.toString());
        aspirante.setNombreUsuarioGenerado(authExistente.getNombreUsuario());
        aspirante.setFechaEnvioCredenciales(LocalDateTime.now());
        aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "¡Ya eres socio de PhotoBogota!",
                construirHtmlAscensoSocio(aspirante.getNombres(), aspirante.getApellidos(),
                        aspirante.getCodigo(), authExistente.getNombreUsuario()));

        try {
            notificacionService.notificarSistema(
                    authExistente.getNombreUsuario(),
                    "¡Ya eres socio de PhotoBogota!",
                    "Tu solicitud (ticket " + aspirante.getCodigo()
                            + ") fue aprobada. Tu cuenta ya tiene el rol de socio; inicia sesión con tu usuario y contraseña de siempre.");
        } catch (Exception e) {
            log.error("Error creando la notificación in-app para {}: {}", authExistente.getNombreUsuario(), e.getMessage());
        }

        log.info("Aspirante {} ascendido de MIEMBRO a SOCIO por {} (cuenta existente: {})",
                aspirante.getId(), responsable, authExistente.getNombreUsuario());
    }

    // Caso original: no existía ninguna cuenta con ese email, se crea desde
    // cero (Usuario + UsuarioAuth) con rol SOCIO y contraseña temporal.
    private void crearCuentaSocioDesdeCero(Aspirante aspirante, String responsable) {
        String nombreUsuario = generarNombreUsuarioUnico(aspirante);
        String passwordTemporal = generarPasswordTemporal();

        ObjectId nuevoId = new ObjectId();

        CrearUsuarioRequestDTO datosUsuario = new CrearUsuarioRequestDTO();
        datosUsuario.setNombresCompletos((aspirante.getNombres() + " " + aspirante.getApellidos()).trim());
        datosUsuario.setFechaNacimiento(aspirante.getFechaNacimiento());
        datosUsuario.setEmail(aspirante.getEmail());
        datosUsuario.setNombreUsuario(nombreUsuario);
        datosUsuario.setRol(Rol.SOCIO.name());

        Usuario nuevoUsuario = usuarioFactory.crearUsuario(nuevoId, datosUsuario, Rol.SOCIO);

        UsuarioAuth nuevaAuth = UsuarioAuth.builder()
                .id(nuevoId)
                .nombreUsuario(nombreUsuario)
                .email(aspirante.getEmail())
                .rol(Rol.SOCIO)
                .contrasena(passwordEncoder.encode(passwordTemporal))
                .build();

        try {
            usuarioRepository.save(nuevoUsuario);
            usuarioAuthRepository.save(nuevaAuth);
        } catch (Exception e) {
            log.error("Error crítico creando la cuenta de socio para aspirante {}: {}", aspirante.getId(), e.getMessage());
            throw new RuntimeException("No se pudo crear la cuenta de socio", e);
        }

        aspirante.setEstado(EstadoAspirante.APROBADO);
        aspirante.setUsuarioCreadoId(nuevoId.toString());
        aspirante.setNombreUsuarioGenerado(nombreUsuario);
        aspirante.setFechaEnvioCredenciales(LocalDateTime.now());
        aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "Tus credenciales de acceso - PhotoBogota",
                construirHtmlCredenciales(aspirante.getNombres(), aspirante.getApellidos(),
                        aspirante.getCodigo(), nombreUsuario, passwordTemporal));

        // Notificación in-app: apenas inicie sesión, verá el mensaje de bienvenida.
        try {
            notificacionService.notificarSistema(
                    nombreUsuario,
                    "¡Ya eres socio de PhotoBogota!",
                    "Tu cuenta fue creada exitosamente (ticket " + aspirante.getCodigo()
                            + "). Revisa tu correo para ver tus credenciales, el manual del socio y el contacto de soporte.");
        } catch (Exception e) {
            log.error("Error creando la notificación in-app para {}: {}", nombreUsuario, e.getMessage());
        }

        log.info("Credenciales enviadas para aspirante {} por {}. Usuario generado: {}", aspirante.getId(), responsable, nombreUsuario);
    }

    // Genera un nombreUsuario a partir del correo (parte antes del @),
    // sanitizado, y le agrega un sufijo numérico si ya está en uso.
    private String generarNombreUsuarioUnico(Aspirante aspirante) {
        String base = aspirante.getEmail().split("@")[0]
                .toLowerCase()
                .replaceAll("[^a-z0-9._]", "");
        if (base.isBlank()) {
            base = "socio";
        }

        String candidato = base;
        int intento = 1;
        while (usuarioAuthRepository.existsByNombreUsuario(candidato)) {
            candidato = base + intento;
            intento++;
        }
        return candidato;
    }

    private String generarPasswordTemporal() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    @Override
    public AspiranteResponseDTO reenviarDocumentos(String codigo, ReenvioDocumentosDTO request) {
        Aspirante aspirante = buscarPorCodigo(codigo);

        if (aspirante.getEstado() != EstadoAspirante.EN_CORRECCION) {
            throw new OperacionInvalidaException(
                    "Solo puedes reenviar documentos si tu solicitud está en estado de corrección");
        }

        aspirante.setRutaArchivo(request.getRutaArchivo());
        if (request.getTipoArchivo() != null) {
            aspirante.setTipoArchivo(request.getTipoArchivo());
        }
        aspirante.setEstado(EstadoAspirante.PENDIENTE);
        aspirante.setFechaReenvio(LocalDateTime.now());
        aspirante.setVecesCorregida(
                (aspirante.getVecesCorregida() == null ? 0 : aspirante.getVecesCorregida()) + 1);

        Aspirante guardado = aspiranteRepository.save(aspirante);

        enviarCorreoSeguro(aspirante.getEmail(),
                "Recibimos tus documentos - PhotoBogota",
                construirHtmlReenvioConfirmado(aspirante.getNombres(), aspirante.getApellidos(), aspirante.getCodigo()));

        log.info("Aspirante con código {} reenvió documentos, vuelve a PENDIENTE", codigo);
        return aspiranteMapper.toResponse(guardado);
    }

    @Override
    public AspiranteResponseDTO agregarComentarioInterno(String id, String texto, String autor) {
        Aspirante aspirante = buscarPorId(id);

        Aspirante.ComentarioInterno comentario = Aspirante.ComentarioInterno.builder()
                .autor(autor)
                .texto(texto)
                .fecha(LocalDateTime.now())
                .build();

        aspirante.getComentariosInternos().add(comentario);
        Aspirante guardado = aspiranteRepository.save(aspirante);

        log.info("Comentario interno agregado a aspirante {} por {}", id, autor);
        return aspiranteMapper.toResponse(guardado);
    }

    @Override
    public EstadisticasAspiranteDTO obtenerEstadisticas() {
        List<Aspirante> todos = aspiranteRepository.findAll();

        long pendientes = todos.stream().filter(a -> a.getEstado() == EstadoAspirante.PENDIENTE).count();
        long enCorreccion = todos.stream().filter(a -> a.getEstado() == EstadoAspirante.EN_CORRECCION).count();
        long enEnvioCredenciales = todos.stream().filter(a -> a.getEstado() == EstadoAspirante.ENVIO_CREDENCIALES).count();
        long aprobadas = todos.stream().filter(a -> a.getEstado() == EstadoAspirante.APROBADO).count();
        long rechazadas = todos.stream().filter(a -> a.getEstado() == EstadoAspirante.RECHAZADO).count();

        return EstadisticasAspiranteDTO.builder()
                .total(todos.size())
                .pendientes(pendientes)
                .enCorreccion(enCorreccion)
                .enEnvioCredenciales(enEnvioCredenciales)
                .aprobadas(aprobadas)
                .rechazadas(rechazadas)
                .procesadas(aprobadas + rechazadas + enEnvioCredenciales)
                .build();
    }

    @Override
    public AspiranteResponseDTO actualizarEstado(String id, EstadoAspirante estado) {
        Aspirante aspirante = buscarPorId(id);

        if (aspirante.getEstado() == estado) {
            throw new OperacionInvalidaException("El aspirante ya se encuentra en estado: " + estado);
        }

        aspirante.setEstado(estado);
        log.info("Estado del aspirante {} actualizado manualmente a {}", id, estado);

        return aspiranteMapper.toResponse(aspiranteRepository.save(aspirante));
    }

    // --- Utilidades privadas ---

    private Aspirante buscarPorId(String id) {
        return aspiranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aspirante no encontrado con id: " + id));
    }

    private Aspirante buscarPorCodigo(String codigo) {
        return aspiranteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Aspirante no encontrado con código: " + codigo));
    }

    private void registrarDecision(Aspirante aspirante, String motivo, String responsable) {
        aspirante.setMotivoDecision(motivo);
        aspirante.setDecididoPor(responsable);
        aspirante.setFechaDecision(LocalDateTime.now());
    }

    // Solo se puede aprobar, rechazar o pedir corrección mientras la solicitud
    // está en revisión (PENDIENTE o EN_CORRECCION). Una vez pasa a un estado
    // terminal (APROBADO, RECHAZADO, ENVIO_CREDENCIALES) ya no se puede
    // reprocesar por esta vía.
    private void validarTransicionDesdeRevision(EstadoAspirante estadoActual) {
        if (estadoActual != EstadoAspirante.PENDIENTE && estadoActual != EstadoAspirante.EN_CORRECCION) {
            throw new OperacionInvalidaException(
                    "No se puede procesar una solicitud que ya fue decidida (estado actual: " + estadoActual + ")");
        }
    }

    private void enviarCorreoSeguro(String destinatario, String asunto, String html) {
        try {
            emailService.enviarCorreoHtml(destinatario, asunto, html);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
        }
    }
}