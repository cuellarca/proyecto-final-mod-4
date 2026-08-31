package com.nurtricenter.logisticdelivery.domain.entrega.event;

import com.nurtricenter.logisticdelivery.domain.entrega.ConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;

import java.time.Instant;

/**
 * Evento: la entrega quedo confirmada con constancia (HU-4). Lleva la {@code ConstanciaDeEntrega}
 * completa para que el read model (HU-6) pueda servir la evidencia sin consultar el lado de escritura.
 */
public record EntregaConfirmada(
        EntregaId entregaId,
        PacienteId pacienteId,
        PaqueteId paqueteId,
        ConstanciaDeEntrega constancia,
        Instant occurredOn) implements DomainEvent {
}
