package com.nurtricenter.logisticdelivery.domain.shared;

import java.time.Instant;

/**
 * Hecho relevante del dominio que ya ocurrio (se nombra en pasado).
 * Los agregados lo emiten cuando su estado cambia por una regla de negocio.
 */
public interface DomainEvent {

    /** Momento en que ocurrio el hecho. */
    Instant occurredOn();
}
