package com.nurtricenter.logisticdelivery.infrastructure.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositorio Spring Data del Outbox. */
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    /** Filas pendientes de publicar, en orden de creacion (para el drenado @Scheduled de la Fase 4). */
    List<OutboxJpaEntity> findByPublishedFalseOrderByCreatedAtAsc(Limit limite);
}
