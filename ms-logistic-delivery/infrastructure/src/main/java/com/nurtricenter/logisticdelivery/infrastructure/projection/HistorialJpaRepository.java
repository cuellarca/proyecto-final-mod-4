package com.nurtricenter.logisticdelivery.infrastructure.projection;

import com.nurtricenter.logisticdelivery.infrastructure.projection.entity.HistorialEntregaPacienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Repositorio Spring Data del read model del historial de entregas por paciente. */
public interface HistorialJpaRepository extends JpaRepository<HistorialEntregaPacienteJpaEntity, UUID> {

    List<HistorialEntregaPacienteJpaEntity> findByPacienteIdAndUltimoEventoEnBetweenOrderByUltimoEventoEnDesc(
            String pacienteId, Instant desde, Instant hasta);
}
