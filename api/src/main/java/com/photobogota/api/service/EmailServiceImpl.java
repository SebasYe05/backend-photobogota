package com.photobogota.api.service;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo electrónico simple (texto plano).
     */
    @Override
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
    @Override
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
    @Override
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
                        background: #9b8dcf;
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
                        <div class="logo"> PhotoBogota</div>
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
                            <p> <strong>Importante:</strong> Si no solicitaste este cambio,
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

    /**
     * Construye el HTML del correo con el código de verificación para
     * solicitar la eliminación de la cuenta.
     */
    @Override
    public String construirHtmlSolicitudEliminacion(String nombreUsuario, String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Eliminar Cuenta - PhotoBogota</title>
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
                        background: #d9534f;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 { color: #ffffff; font-size: 26px; margin-bottom: 5px; }
                    .header p { color: #f5d5d4; font-size: 14px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .content h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }
                    .content p { color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .code-box {
                        background-color: #f8f9fa;
                        border: 2px dashed #d9534f;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 25px 0;
                    }
                    .code { font-size: 32px; font-weight: bold; color: #d9534f; letter-spacing: 5px; }
                    .consequences {
                        background-color: #fdecea;
                        border-left: 4px solid #d9534f;
                        padding: 18px;
                        margin-top: 20px;
                        text-align: left;
                        border-radius: 4px;
                    }
                    .consequences p { color: #7a2925; font-size: 14px; margin: 6px 0; }
                    .consequences strong { color: #58201c; }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin-top: 20px;
                        text-align: left;
                        border-radius: 4px;
                    }
                    .warning p { color: #856404; font-size: 14px; margin: 0; }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p { color: #999999; font-size: 12px; margin: 5px 0; }
                    .logo { font-size: 24px; font-weight: bold; color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo"> PhotoBogota</div>
                        <h1>Eliminar mi cuenta</h1>
                        <p>Confirmación necesaria</p>
                    </div>
                    <div class="content">
                        <h2>Hola, %NOMBRE%</h2>
                        <p>Recibimos una solicitud para eliminar tu cuenta de PhotoBogota.
                           Para confirmarla, ingresá el siguiente código de verificación:</p>

                        <div class="code-box">
                            <span class="code">%CODIGO%</span>
                        </div>

                        <p>Este código <strong>expira en 15 minutos</strong> por seguridad.</p>

                        <div class="consequences">
                            <p><strong>Qué va a pasar si confirmás:</strong></p>
                            <p>• Tu cuenta quedará desactivada de inmediato.</p>
                            <p>• Vas a tener <strong>30 días</strong> para recuperarla iniciando sesión nuevamente.</p>
                            <p>• Pasado ese plazo, tus datos personales se anonimizarán de forma permanente
                               y no podrán recuperarse, conservando únicamente estadísticas generales
                               y anónimas de la plataforma.</p>
                        </div>

                        <div class="warning">
                            <p> <strong>Importante:</strong> Si no solicitaste esto, ignorá este correo.
                            Tu cuenta seguirá activa con normalidad.</p>
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

    /**
     * Construye el HTML del correo que confirma que la eliminación quedó
     * programada, con la fecha límite de recuperación.
     */
    @Override
    public String construirHtmlConfirmacionEliminacion(String nombreUsuario, String fechaProgramada) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Eliminación Programada - PhotoBogota</title>
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
                    .header { background: #d9534f; padding: 30px; text-align: center; }
                    .header h1 { color: #ffffff; font-size: 26px; margin-bottom: 5px; }
                    .header p { color: #f5d5d4; font-size: 14px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .content h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }
                    .content p { color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .date-box {
                        background-color: #f8f9fa;
                        border: 2px dashed #d9534f;
                        border-radius: 8px;
                        padding: 18px;
                        margin: 25px 0;
                    }
                    .date-box span { font-size: 20px; font-weight: bold; color: #d9534f; }
                    .recover-box {
                        background-color: #eaf5ea;
                        border-left: 4px solid #22c55e;
                        padding: 18px;
                        margin-top: 20px;
                        text-align: left;
                        border-radius: 4px;
                    }
                    .recover-box p { color: #1f5c2f; font-size: 14px; margin: 0; }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p { color: #999999; font-size: 12px; margin: 5px 0; }
                    .logo { font-size: 24px; font-weight: bold; color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo"> PhotoBogota</div>
                        <h1>Eliminación confirmada</h1>
                        <p>Tu cuenta fue desactivada</p>
                    </div>
                    <div class="content">
                        <h2>Hola, %NOMBRE%</h2>
                        <p>Confirmamos tu solicitud de eliminación de cuenta. Tu cuenta quedó
                           <strong>desactivada</strong> y tus datos personales se eliminarán
                           definitivamente el:</p>

                        <div class="date-box">
                            <span>%FECHA%</span>
                        </div>

                        <div class="recover-box">
                            <p> <strong>¿Te arrepentiste?</strong> Podés recuperar tu cuenta iniciando
                            sesión nuevamente con tus credenciales en cualquier momento antes de esa fecha.</p>
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
            .replace("%FECHA%", fechaProgramada);
    }

    /**
     * Construye el HTML del correo que confirma que la eliminación fue
     * cancelada y la cuenta quedó reactivada.
     */
    @Override
    public String construirHtmlCancelacionEliminacion(String nombreUsuario) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Cuenta Reactivada - PhotoBogota</title>
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
                    .header { background: #22c55e; padding: 30px; text-align: center; }
                    .header h1 { color: #ffffff; font-size: 26px; margin-bottom: 5px; }
                    .header p { color: #dff6e6; font-size: 14px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .content h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }
                    .content p { color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p { color: #999999; font-size: 12px; margin: 5px 0; }
                    .logo { font-size: 24px; font-weight: bold; color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo"> PhotoBogota</div>
                        <h1>¡Cuenta reactivada!</h1>
                        <p>Nos alegra tenerte de vuelta</p>
                    </div>
                    <div class="content">
                        <h2>Hola, %NOMBRE%</h2>
                        <p>Tu solicitud de eliminación de cuenta fue <strong>cancelada</strong> y tu
                           cuenta volvió a estar completamente activa. No vamos a eliminar tus datos.</p>
                        <p>Si no reconocés esta acción, contactanos de inmediato.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 PhotoBogota. Todos los derechos reservados.</p>
                        <p>Este correo fue enviado automáticamente, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("%NOMBRE%", nombreUsuario);
    }

    /**
     * Construye el HTML del correo final que confirma la eliminación
     * definitiva de la cuenta y la anonimización de los datos personales.
     */
    @Override
    public String construirHtmlEliminacionCompletada(String nombreUsuario) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Cuenta Eliminada - PhotoBogota</title>
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
                    .header { background: #4a4a4a; padding: 30px; text-align: center; }
                    .header h1 { color: #ffffff; font-size: 26px; margin-bottom: 5px; }
                    .header p { color: #d9d9d9; font-size: 14px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .content h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }
                    .content p { color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #eeeeee;
                    }
                    .footer p { color: #999999; font-size: 12px; margin: 5px 0; }
                    .logo { font-size: 24px; font-weight: bold; color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo"> PhotoBogota</div>
                        <h1>Cuenta eliminada</h1>
                        <p>Proceso completado</p>
                    </div>
                    <div class="content">
                        <h2>Hola, %NOMBRE%</h2>
                        <p>Tu cuenta y tus datos personales fueron eliminados de forma definitiva de
                           PhotoBogota, tal como lo solicitaste. Conservamos únicamente estadísticas
                           generales y anónimas de la plataforma.</p>
                        <p>Gracias por haber sido parte de nuestra comunidad. ¡Esperamos verte de
                           nuevo en el futuro!</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 PhotoBogota. Todos los derechos reservados.</p>
                        <p>Este correo fue enviado automáticamente, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("%NOMBRE%", nombreUsuario);
    }
}
