package com.photobogota.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void enviarCorreo_enviaUnSimpleMailMessageConRemitenteFijo() {
        emailService.enviarCorreo("destino@example.com", "Asunto", "Cuerpo");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("destino@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(captor.getValue().getText()).isEqualTo("Cuerpo");
        assertThat(captor.getValue().getFrom()).isEqualTo("photobogota123@gmail.com");
    }

    @Test
    void enviarCorreoHtml_enviaMensajeHtml() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.enviarCorreoHtml("destino@example.com", "Asunto HTML", "<h1>Hola</h1>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void enviarCorreoHtml_errorDeMail_rupdaComoRuntimeException() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doAnswer(invocation -> {
            throw new MessagingException("no hay conexión SMTP");
        }).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.enviarCorreoHtml("destino@example.com", "Asunto", "<p>Hola</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar el correo electrónico");
    }

    @Test
    void construirHtmlRecuperacion_reemplazaLosPlaceholders() {
        String html = emailService.construirHtmlRecuperacion("juanperez", "487568");

        assertThat(html).contains("juanperez");
        assertThat(html).contains("487568");
        assertThat(html).doesNotContain("%NOMBRE%", "%CODIGO%");
    }

    @Test
    void construirHtmlSolicitudEliminacion_incluyeElCodigo() {
        String html = emailService.construirHtmlSolicitudEliminacion("juanperez", "123456");

        assertThat(html).contains("juanperez");
        assertThat(html).contains("123456");
        assertThat(html).contains("eliminar tu cuenta de PhotoBogota");
    }

    @Test
    void construirHtmlConfirmacionEliminacion_incluyeLaFechaProgramada() {
        String html = emailService.construirHtmlConfirmacionEliminacion("juanperez", "30/09/2026");

        assertThat(html).contains("30/09/2026");
        assertThat(html).doesNotContain("%FECHA%");
    }

    @Test
    void construirHtmlCancelacionEliminacion_confirmoLaReactivacion() {
        String html = emailService.construirHtmlCancelacionEliminacion("juanperez");

        assertThat(html).contains("juanperez");
        assertThat(html).contains("volvió a estar completamente activa");
    }

    @Test
    void construirHtmlEliminacionCompletada_confirmoLaEliminacion() {
        String html = emailService.construirHtmlEliminacionCompletada("juanperez");

        assertThat(html).contains("juanperez");
        assertThat(html).contains("tus datos personales fueron eliminados");
    }

    @Test
    void enviarCorreoHtml_nullEnElParametroCuerpo_lanzaIllegalArgument() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatThrownBy(() -> emailService.enviarCorreoHtml("destino@example.com", "Asunto", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Text must not be null");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}