package com.nurtricenter.logisticdelivery.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.EventoIntegracion;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.RabbitTopologyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drenado del Outbox hacia RabbitMQ (entrega al-menos-una-vez). El broker y el repositorio son los
 * seams; el reloj entra fijo (testing-rules.md, LIMITES). El comportamiento critico es que una fila
 * que no se pudo publicar NO quede marcada como publicada: de eso depende el reintento.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorOutboxSchedulerTest {

    private static final Instant AHORA = Instant.parse("2026-07-10T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final UUID ID_FILA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    OutboxJpaRepository outbox;
    @Mock
    RabbitTemplate rabbitTemplate;

    private OutboxJpaEntity filaPendiente() {
        return new OutboxJpaEntity(ID_FILA, "EntregaConfirmada", "e-1",
                "{\"entregaId\":\"e-1\"}", AHORA);
    }

    private PublicadorOutboxScheduler scheduler() {
        return new PublicadorOutboxScheduler(outbox, rabbitTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()), RELOJ, 50);
    }

    @Test
    @DisplayName("Cada evento sale por la routing key de su tipo")
    void publicaConLaRoutingKeyDelTipo() {
        when(outbox.findByPublishedFalseOrderByCreatedAtAsc(any(Limit.class)))
                .thenReturn(List.of(filaPendiente()));

        scheduler().drenar();

        verify(rabbitTemplate).convertAndSend(eq(RabbitTopologyConfig.EXCHANGE),
                eq("logistica.entrega-confirmada"), any(EventoIntegracion.class));
    }

    @Test
    @DisplayName("Una fila publicada queda marcada con el instante del reloj inyectado")
    void marcaLaFilaComoPublicada() {
        OutboxJpaEntity fila = filaPendiente();
        when(outbox.findByPublishedFalseOrderByCreatedAtAsc(any(Limit.class))).thenReturn(List.of(fila));

        scheduler().drenar();

        assertThat(fila.getPublishedAt()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Si el broker esta caido la fila queda pendiente: se reintenta en el proximo ciclo")
    void noMarcaLaFilaSiElBrokerFalla() {
        OutboxJpaEntity fila = filaPendiente();
        when(outbox.findByPublishedFalseOrderByCreatedAtAsc(any(Limit.class))).thenReturn(List.of(fila));
        doThrow(new AmqpException("broker caido"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        scheduler().drenar();

        assertThat(fila.isPublished()).isFalse();
    }
}
