package com.nurtricenter.logisticdelivery.application.usecase.query;

import java.time.Instant;

/** Vista de lectura de una entrega en el historial de un paciente (HU-6). */
public record HistorialEntregaView(
        String entregaId,
        String pacienteId,
        String paqueteId,
        String estado,
        String motivoFallo,
        Integer intentos,
        Instant ultimoEventoEn) {
}
