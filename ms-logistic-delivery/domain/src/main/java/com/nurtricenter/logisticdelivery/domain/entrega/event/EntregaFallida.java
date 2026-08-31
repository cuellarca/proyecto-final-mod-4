package com.nurtricenter.logisticdelivery.domain.entrega.event;

import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;

import java.time.Instant;

/** Evento: la entrega no pudo concretarse y se registro su motivo (HU-5). */
public record EntregaFallida(
        EntregaId entregaId,
        PacienteId pacienteId,
        MotivoFallo motivo,
        Instant occurredOn) implements DomainEvent {
}
