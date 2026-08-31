package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Published language de los eventos de integracion que Logistica publica en RabbitMQ. Es un sobre
 * versionable: metadatos ({@code eventId}, {@code type}, {@code occurredAt}, {@code version}) mas el
 * {@code payload} del evento de dominio. Los consumidores lo usan para enrutar e idempotencia.
 */
public record EventoIntegracion(
        String eventId,
        String type,
        Instant occurredAt,
        int version,
        JsonNode payload) {

    public static final int VERSION_ACTUAL = 1;
}
