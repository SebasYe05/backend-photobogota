package com.photobogota.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.dto.ReenvioDocumentosDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.exception.AspiranteAlreadyExistsException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.AspiranteMapper;
import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.repository.AspiranteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AspiranteServiceImpl implements IAspiranteService {

    private final AspiranteRepository aspiranteRepository;
    private final AspiranteMapper aspiranteMapper;
    private final IEmailService emailService;

    @Override
    public AspiranteResponseDTO crearSolicitud(SolicitudAspiranteDTO request) {
        if (aspiranteRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AspiranteAlreadyExistsException("Ya existe una solicitud con este email");
        }

        if (aspiranteRepository.findByNit(request.getNit()).isPresent()) {
            throw new AspiranteAlreadyExistsException("Ya existe una solicitud con este NIT/RUT");
        }

        String codigoGenerado = generarCodigo();

        Aspirante aspirante = mapearSolicitud(request, codigoGenerado);
        Aspirante savedAspirante = aspiranteRepository.save(aspirante);

        String htmlContent = construirHtmlSolicitud(
            request.getNombres(),
            request.getApellidos(),
            codigoGenerado
        );

        try {
            emailService.enviarCorreoHtml(
                request.getEmail(),
                "Tu código de solicitud - PhotoBogota",
                htmlContent
            );
            log.info("Correo enviado a {} con código {}", request.getEmail(), codigoGenerado);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", request.getEmail(), e.getMessage());
        }

        log.info("Solicitud creada para {} con código {}", request.getEmail(), codigoGenerado);

        return aspiranteMapper.toResponse(savedAspirante);
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

    private String construirHtmlSolicitud(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "¡Hola, " + nombres + " " + apellidos + "!",
            "Tu solicitud para convertirte en socio de PhotoBogota ha sido registrada exitosamente.",
            codigo,
            "Guarda este código para dar seguimiento a tu solicitud. Nuestro equipo la revisará y te contactaremos pronto."
        );
    }

    private String construirHtmlCorreccion(String nombres, String apellidos, String codigo, String motivo) {
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Un moderador de PhotoBogota revisó tu solicitud y necesita que corrijas algunos datos o documentos antes de continuar.",
            codigo,
            "<strong>Motivo:</strong> " + motivo
                + "<br/><br/>Ingresa con tu código de solicitud a la sección \"Consultar solicitud\" para reenviar tu documentación."
        );
    }

    private String construirHtmlRechazo(String nombres, String apellidos, String codigo, String motivo) {
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Lamentamos informarte que tu solicitud para ser socio de PhotoBogota fue rechazada.",
            codigo,
            "<strong>Motivo:</strong> " + motivo
        );
    }

    private String construirHtmlAprobado(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "¡Felicidades, " + nombres + " " + apellidos + "!",
            "Tu solicitud para ser socio de PhotoBogota fue aprobada. Estamos preparando la creación de tu cuenta.",
            codigo,
            "En los próximos días recibirás un correo con tus credenciales de acceso a la plataforma."
        );
    }

    private String construirHtmlReenvioConfirmado(String nombres, String apellidos, String codigo) {
        return construirPlantillaBase(
            "Hola, " + nombres + " " + apellidos,
            "Recibimos los documentos que reenviaste para tu solicitud de ingreso a PhotoBogota.",
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
        Aspirante aspirante = aspiranteRepository.findByEmail(email)
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
    // reprocesar por esta vía; para eso existe actualizarEstado (solo ADMIN).
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
