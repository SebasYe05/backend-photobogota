package com.photobogota.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.EmailRequestDto;
import com.photobogota.api.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/envio")
    public ResponseEntity<String> enviar(@RequestBody EmailRequestDto request) {
        try {
            emailService.enviarCorreo(request.getTo(), request.getSubject(), request.getContent());
            return ResponseEntity.ok("¡Correo enviado con éxito!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al enviar: " + e.getMessage());
        }
    }



}