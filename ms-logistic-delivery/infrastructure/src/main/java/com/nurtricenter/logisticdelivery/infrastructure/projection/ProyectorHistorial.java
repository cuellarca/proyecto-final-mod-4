package com.nurtricenter.logisticdelivery.infrastructure.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.EventoIntegracion;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RegistroDeIdempotencia;
import com.nurtricenter.logisticdelivery.infrastructure.projection.entity.HistorialEntregaPacienteJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Proyector del read model del historial de entregas (HU-6): consume los eventos de la entrega desde
 * RabbitMQ y hace <b>upsert</b> en {@code historial_entrega_paciente}. Deduplica por {@code eventId}
 * (idempotencia). Es el unico que escribe el read model; las consultas solo leen.
 */
@Component
public class ProyectorHistorial {

    private static final Logger log = LoggerFactory.getLogger(ProyectorHistorial.class);
    private static final String CONSUMIDOR = "proyector-historial";

    private final HistorialJpaRepository repositorio;
    private final RegistroDeIdempotencia idempotencia;
    private final Clock clock;

    public ProyectorHistorial(HistorialJpaRepository repositorio, RegistroDeIdempotencia idempotencia, Clock clock) {
        this.repositorio = repositorio;
        this.idempotencia = idempotencia;
        this.clock = clock;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_PROYECCION)
    @Transactional
    public void onEvento(EventoIntegracion evento) {
        if (!idempotencia.primeraVez(CONSUMIDOR, evento.eventId())) {
            return;
        }
        JsonNode payload = evento.payload();
        String entregaId = payload.path("entregaId").asText(null);
        if (entregaId == null || entregaId.isBlank()) {
            return; // eventos sin entrega (p. ej. de ruta) no proyectan al historial de paciente
        }

        UUID id = UUID.fromString(entregaId);
        String pacienteId = payload.path("pacienteId").asText(null);
        HistorialEntregaPacienteJpaEntity fila = repositorio.findById(id)
                .orElseGet(() -> new HistorialEntregaPacienteJpaEntity(id, pacienteId));
        if (pacienteId != null) {
            fila.setPacienteId(pacienteId);
        }
        fila.setUltimoEventoEn(evento.occurredAt());
        fila.setActualizadoEn(Instant.now(clock));

        switch (evento.type()) {
            case "EntregaConfirmada" -> aplicarConfirmada(fila, payload);
            case "EntregaFallida" -> aplicarFallida(fila, payload);
            case "EntregaReprogramada" -> aplicarReprogramada(fila, payload);
            case "EntregaNoConcretada" -> fila.setEstado("NO_CONCRETADA");
            default -> {
                return;
            }
        }

        repositorio.save(fila);
        log.info("[proyector] historial actualizado: entrega {} paciente {} -> {}",
                entregaId, fila.getPacienteId(), fila.getEstado());
    }

    private void aplicarConfirmada(HistorialEntregaPacienteJpaEntity fila, JsonNode payload) {
        fila.setEstado("CONFIRMADA");
        fila.setPaqueteId(payload.path("paqueteId").asText(null));
        fila.setMotivoFallo(null);

        JsonNode constancia = payload.path("constancia");
        if (!constancia.isMissingNode()) {
            fila.setConstanciaTimestamp(Instant.parse(constancia.path("timestamp").asText()));
            fila.setConstanciaLat(constancia.path("geolocalizacion").path("lat").asDouble());
            fila.setConstanciaLon(constancia.path("geolocalizacion").path("lon").asDouble());
            fila.setConstanciaUrl(constancia.path("urlEvidencia").asText(null));
            fila.setConstanciaReceptor(constancia.path("nombreReceptor").asText(null));
        }
    }

    private void aplicarFallida(HistorialEntregaPacienteJpaEntity fila, JsonNode payload) {
        fila.setEstado("FALLIDA");
        fila.setMotivoFallo(payload.path("motivo").asText(null));
    }

    private void aplicarReprogramada(HistorialEntregaPacienteJpaEntity fila, JsonNode payload) {
        fila.setEstado("REPROGRAMADA");
        fila.setIntentos(payload.path("intento").asInt());
    }
}
