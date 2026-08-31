package com.nurtricenter.logisticdelivery.domain.entrega.event;

import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;

import java.time.Instant;

/** Evento: se acepto un reintento de la entrega (HU-5). */
public record EntregaReprogramada(
        EntregaId entregaId,
        PacienteId pacienteId,
        int intento,
        Instant occurredOn) implements DomainEvent {
}
