package com.nurtricenter.logisticdelivery.domain.ruta.event;

import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;

import java.time.Instant;

/** Evento: la ruta del dia quedo planificada y lista para ejecutarse (HU-1/HU-3). */
public record RutaDeEntregaPlanificada(
        RutaId rutaId,
        RepartidorId repartidorId,
        Fecha fecha,
        int cantidadParadas,
        Instant occurredOn) implements DomainEvent {
}
