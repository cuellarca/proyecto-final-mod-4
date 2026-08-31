package com.nurtricenter.logisticdelivery.infrastructure.projection;

import com.nurtricenter.logisticdelivery.application.port.out.HistorialReadModel;
import com.nurtricenter.logisticdelivery.application.usecase.query.ConstanciaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.HistorialEntregaView;
import com.nurtricenter.logisticdelivery.infrastructure.projection.entity.HistorialEntregaPacienteJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de lectura del puerto {@code HistorialReadModel}: consulta el read model y mapea a
 * vistas. No toca el lado de escritura (CQRS, HU-6).
 */
@Component
public class HistorialReadModelJpa implements HistorialReadModel {

    private final HistorialJpaRepository repositorio;

    public HistorialReadModelJpa(HistorialJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<HistorialEntregaView> historialDePaciente(String pacienteId, Instant desde, Instant hasta) {
        return repositorio
                .findByPacienteIdAndUltimoEventoEnBetweenOrderByUltimoEventoEnDesc(pacienteId, desde, hasta)
                .stream()
                .map(this::toHistorialView)
                .toList();
    }

    @Override
    public Optional<ConstanciaView> constanciaDe(String entregaId) {
        return repositorio.findById(UUID.fromString(entregaId))
                .filter(e -> e.getConstanciaUrl() != null)
                .map(this::toConstanciaView);
    }

    private HistorialEntregaView toHistorialView(HistorialEntregaPacienteJpaEntity e) {
        return new HistorialEntregaView(
                e.getEntregaId().toString(),
                e.getPacienteId(),
                e.getPaqueteId(),
                e.getEstado(),
                e.getMotivoFallo(),
                e.getIntentos(),
                e.getUltimoEventoEn());
    }

    private ConstanciaView toConstanciaView(HistorialEntregaPacienteJpaEntity e) {
        return new ConstanciaView(
                e.getEntregaId().toString(),
                e.getConstanciaTimestamp(),
                e.getConstanciaLat(),
                e.getConstanciaLon(),
                e.getConstanciaUrl(),
                e.getConstanciaReceptor());
    }
}
