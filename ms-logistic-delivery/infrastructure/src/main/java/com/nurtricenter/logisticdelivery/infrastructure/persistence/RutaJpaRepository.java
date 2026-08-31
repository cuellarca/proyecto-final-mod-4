package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.infrastructure.persistence.entity.RutaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Repositorio Spring Data del agregado ruta (detalle de infraestructura). */
public interface RutaJpaRepository extends JpaRepository<RutaJpaEntity, UUID> {

    List<RutaJpaEntity> findByRepartidorIdAndFecha(String repartidorId, LocalDate fecha);
}
