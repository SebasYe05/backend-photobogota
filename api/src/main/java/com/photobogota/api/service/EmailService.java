package com.photobogota.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envía un correo electrónico simple (texto plano).
     */
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mensaje.setFrom("photobogota123@gmail.com");

        mailSender.send(mensaje);
    }

    /**
     * Envía un correo electrónico con contenido HTML.
     */
    public void enviarCorreoHtml(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            helper.setFrom("photobogota123@gmail.com");

            mailSender.send(mensaje);
            log.info("Correo HTML enviado exitosamente a: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Error al enviar correo HTML a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar el correo electrónico", e);
        }
    }

    /**
     * Construye el HTML para el correo de recuperación de contraseña.
     * 
     * NOTA: Se usa .replace() en lugar de .formatted() porque el HTML contiene
     * caracteres # en los colores CSS que Java interpreta como especificadores
     * de formato, causando FormatFlagsConversionMismatchException.
     *
     * @param nombreUsuario Nombre del usuario
     * @param codigo        Código de recuperación
     * @return String con el HTML del correo
     */
    public String construirHtmlRecuperacion(String nombreUsuario, String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recuperar Contraseña - PhotoBogota</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
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
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        color: #ffffff;
                        font-size: 28px;
                        margin-bottom: 5px;
                    }
                    .header p {
                        color: #e0e0e0;
                        font-size: 14px;
                    }
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .content h2 {
                        color: #333333;
                        font-size: 22px;
                        margin-bottom: 20px;
                    }
                    .content p {
                        color: #666666;
                        font-size: 16px;
                        line-height: 1.6;
                        margin-bottom: 25px;
                    }
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
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin-top: 25px;
                        text-align: left;
                        border-radius: 4px;
                    }
                    .warning p {
                        color: #856404;
                        font-size: 14px;
                        margin: 0;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p {
                        color: #999999;
                        font-size: 12px;
                        margin: 5px 0;
                    }
                    .footer a {
                        color: #667eea;
                        text-decoration: none;
                    }
                    .logo {
                        font-size: 24px;
                        font-weight: bold;
                        color: #ffffff;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">📸 PhotoBogota</div>
                        <h1>Recuperar Contraseña</h1>
                        <p>Restablece el acceso a tu cuenta</p>
                    </div>
                    <div class="content">
                        <h2>¡Hola, %NOMBRE%!</h2>
                        <p>Hemos recibido una solicitud para restablecer tu contraseña.
                           Utiliza el siguiente código de verificación:</p>

                        <div class="code-box">
                            <span class="code">%CODIGO%</span>
                        </div>

                        <p>Este código <strong>expira en 15 minutos</strong> por seguridad.</p>

                        <div class="warning">
                            <p>⚠️ <strong>Importante:</strong> Si no solicitaste este cambio,
                            puedes ignorar este correo. Tu contraseña actual seguirá funcionando.</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2024 PhotoBogota. Todos los derechos reservados.</p>
                        <p>Este correo fue enviado automáticamente, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("%NOMBRE%", nombreUsuario)
            .replace("%CODIGO%", codigo);
    }
}