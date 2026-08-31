package com.nurtricenter.logisticdelivery.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaConfirmada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaFallida;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaNoConcretada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaReprogramada;
import com.nurtricenter.logisticdelivery.domain.ruta.event.RutaDeEntregaPlanificada;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador del puerto {@code PublicadorDeEventos}: escribe cada evento de dominio como una fila del
 * Outbox (serializado a JSON con Jackson). Corre dentro de la transaccion del caso de uso, por lo que
 * el evento se persiste atomicamente junto al agregado (Outbox transaccional manual).
 */
@Component
public class PublicadorDeEventosOutbox implements PublicadorDeEventos {

    private final OutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;

    public PublicadorDeEventosOutbox(OutboxJpaRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(List<DomainEvent> eventos) {
        for (DomainEvent evento : eventos) {
            OutboxJpaEntity fila = new OutboxJpaEntity(
                    UUID.randomUUID(),
                    tipoDe(evento),
                    aggregateIdDe(evento),
                    serializar(evento),
                    evento.occurredOn());
            outbox.save(fila);
        }
    }

    private String serializar(DomainEvent evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + tipoDe(evento), e);
        }
    }

    /** Nombre estable del tipo de evento (published language). */
    private String tipoDe(DomainEvent evento) {
        return evento.getClass().getSimpleName();
    }

    /** Identidad del agregado que emitio el evento, para trazabilidad y proyeccion (Fase 5). */
    private String aggregateIdDe(DomainEvent evento) {
        return switch (evento) {
            case EntregaConfirmada e -> e.entregaId().toString();
            case EntregaFallida e -> e.entregaId().toString();
            case EntregaReprogramada e -> e.entregaId().toString();
            case EntregaNoConcretada e -> e.entregaId().toString();
            case RutaDeEntregaPlanificada e -> e.rutaId().toString();
            default -> null;
        };
    }
}
