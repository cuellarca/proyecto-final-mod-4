package com.nurtricenter.logisticdelivery.domain.entrega.event;

import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;

import java.time.Instant;

/** Evento: se agotaron los reintentos; la entrega queda NO_CONCRETADA (terminal, HU-5). */
public record EntregaNoConcretada(
        EntregaId entregaId,
        PacienteId pacienteId,
        Instant occurredOn) implements DomainEvent {
}
