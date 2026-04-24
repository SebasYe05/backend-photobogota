package com.photobogota.api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.exception.AspiranteAlreadyExistsException;
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
        return aspirante;
    }

    private String generarCodigo() {
        String prefijo = "SOL-";
        String numeros = String.format("%06d", new java.util.Random().nextInt(999999));
        return prefijo + numeros;
    }

    private String construirHtmlSolicitud(String nombres, String apellidos, String codigo) {
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
                        <h2>¡Hola, %NOMBRE%!</h2>
                        <p>Tu solicitud para convertirte en socio de PhotoBogota ha sido registrada exitosamente.</p>
                        <p>Tu código de seguimiento es:</p>
                        <div class="code-box">
                            <span class="code">%CODIGO%</span>
                        </div>
                        <div class="info-box">
                            <p><strong>Guarda este código</strong> para dar seguimiento a tu solicitud.</p>
                            <p>Nuestro equipo revisará tu solicitud y te contactaremos pronto.</p>
                        </div>
                        <p>Gracias por tu interés en ser parte de nuestra comunidad fotográfica.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 PhotoBogota. Todos los derechos reservados.</p>
                        <p>Este correo fue enviado automáticamente.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("%NOMBRE%", nombres + " " + apellidos)
            .replace("%CODIGO%", codigo);
    }

    @Override
    public AspiranteResponseDTO obtenerPorId(String id) {
        Aspirante aspirante = aspiranteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con id: " + id));
        return aspiranteMapper.toResponse(aspirante);
    }

    @Override
    public AspiranteResponseDTO obtenerPorEmail(String email) {
        Aspirante aspirante = aspiranteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con email: " + email));
        return aspiranteMapper.toResponse(aspirante);
    }

    @Override
    public AspiranteResponseDTO obtenerPorCodigo(String codigo) {
        Aspirante aspirante = aspiranteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con código: " + codigo));
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
    public AspiranteResponseDTO aprobarAspirante(String id) {
        return cambiarEstado(id, EstadoAspirante.APROBADO);
    }

    @Override
    public AspiranteResponseDTO rechazarAspirante(String id) {
        return cambiarEstado(id, EstadoAspirante.RECHAZADO);
    }

    @Override
    public AspiranteResponseDTO actualizarEstado(String id, EstadoAspirante estado) {
        return cambiarEstado(id, estado);
    }

    private AspiranteResponseDTO cambiarEstado(String id, EstadoAspirante nuevoEstado) {
        Aspirante aspirante = aspiranteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aspirante no encontrado con id: " + id));
        
        validarTransicionEstado(aspirante.getEstado(), nuevoEstado);
        
        aspirante.setEstado(nuevoEstado);
        
        log.info("Aspirante {} cambió a estado {}", id, nuevoEstado);
        
        return aspiranteMapper.toResponse(aspiranteRepository.save(aspirante));
    }

    private void validarTransicionEstado(EstadoAspirante estadoActual, EstadoAspirante nuevoEstado) {
        if (estadoActual == nuevoEstado) {
            throw new RuntimeException("El aspirante ya se encuentra en estado: " + nuevoEstado);
        }

        if (estadoActual == EstadoAspirante.APROBADO || estadoActual == EstadoAspirante.RECHAZADO) {
            throw new RuntimeException("No se puede cambiar el estado de un aspirante ya procesado (APROBADO/RECHAZADO)");
        }
    }
}
