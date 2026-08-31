package com.nurtricenter.logisticdelivery.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.EventoIntegracion;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Publicador del Outbox: cada cierto intervalo drena las filas pendientes hacia RabbitMQ (serializa
 * el sobre {@link EventoIntegracion}, publica al exchange con la routing key del tipo y marca la fila
 * como publicada). Si la publicacion falla, la fila queda pendiente y se reintenta en el proximo
 * ciclo (entrega al-menos-una-vez; los consumidores deduplican por {@code eventId}).
 */
@Component
public class PublicadorOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutboxScheduler.class);

    private final OutboxJpaRepository outbox;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int batchSize;

    public PublicadorOutboxScheduler(OutboxJpaRepository outbox, RabbitTemplate rabbitTemplate,
                                     ObjectMapper objectMapper, Clock clock,
                                     @Value("${logistic.outbox.batch-size:50}") int batchSize) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${logistic.outbox.poll-delay-ms:2000}")
    public void drenar() {
        List<OutboxJpaEntity> pendientes = outbox.findByPublishedFalseOrderByCreatedAtAsc(Limit.of(batchSize));
        if (pendientes.isEmpty()) {
            return;
        }
        for (OutboxJpaEntity fila : pendientes) {
            try {
                publicar(fila);
                fila.marcarPublicada(Instant.now(clock));
                outbox.save(fila);
            } catch (Exception e) {
                log.warn("[outbox] no se pudo publicar {} ({}); se reintentara. Causa: {}",
                        fila.getEventType(), fila.getId(), e.toString());
            }
        }
    }

    private void publicar(OutboxJpaEntity fila) throws Exception {
        JsonNode payload = objectMapper.readTree(fila.getPayload());
        EventoIntegracion sobre = new EventoIntegracion(
                fila.getId().toString(),
                fila.getEventType(),
                fila.getOccurredAt(),
                EventoIntegracion.VERSION_ACTUAL,
                payload);
        String routingKey = RoutingKeys.paraTipo(fila.getEventType());
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE, routingKey, sobre);
        log.info("[outbox] publicado {} ({}) -> rk={}", fila.getEventType(), fila.getId(), routingKey);
    }
}
