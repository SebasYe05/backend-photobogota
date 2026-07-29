package com.photobogota.api.service;

import com.photobogota.api.dto.ConfirmarEliminacionDTO;
import com.photobogota.api.dto.EstadoEliminacionDTO;
import com.photobogota.api.dto.SolicitarEliminacionDTO;

/**
 * Servicio de autoeliminación de cuenta para usuarios con rol MIEMBRO.
 *
 * Flujo:
 * 1. solicitarEliminacion: genera un código de verificación y lo envía por
 * correo, junto con las consecuencias de eliminar la cuenta.
 * 2. confirmarEliminacion: valida el código, desactiva la cuenta y programa
 * la anonimización definitiva para dentro de 30 días.
 * 3. cancelarEliminacion: revierte la solicitud si el usuario se arrepiente
 * dentro del período de 30 días.
 * 4. obtenerEstado: permite al frontend saber si hay una solicitud activa.
 */
public interface IEliminacionCuentaService {

    String solicitarEliminacion(String nombreUsuario, SolicitarEliminacionDTO dto);

    String confirmarEliminacion(String nombreUsuario, ConfirmarEliminacionDTO dto);

    String cancelarEliminacion(String nombreUsuario);

    EstadoEliminacionDTO obtenerEstado(String nombreUsuario);
}
