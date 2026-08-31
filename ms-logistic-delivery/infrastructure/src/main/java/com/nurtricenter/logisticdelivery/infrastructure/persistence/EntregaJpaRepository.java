package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.EntregaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositorio Spring Data del agregado entrega (detalle de infraestructura). */
public interface EntregaJpaRepository extends JpaRepository<EntregaJpaEntity, UUID> {

    List<EntregaJpaEntity> findByRutaId(UUID rutaId);
}
