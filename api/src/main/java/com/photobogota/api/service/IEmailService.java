package com.photobogota.api.service;

/**
 * Interfaz para el servicio de correo electrónico.
 * Define los métodos para enviar correos y construir plantillas HTML.
 */
public interface IEmailService {

    /**
     * Envía un correo electrónico simple (texto plano).
     *
     * @param destinatario Dirección de correo del destinatario
     * @param asunto       Asunto del correo
     * @param cuerpo       Contenido del correo en texto plano
     */
    void enviarCorreo(String destinatario, String asunto, String cuerpo);

    /**
     * Envía un correo electrónico con contenido HTML.
     *
     * @param destinatario Dirección de correo del destinatario
     * @param asunto       Asunto del correo
     * @param cuerpoHtml   Contenido del correo en formato HTML
     */
    void enviarCorreoHtml(String destinatario, String asunto, String cuerpoHtml);

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
    String construirHtmlRecuperacion(String nombreUsuario, String codigo);
}
