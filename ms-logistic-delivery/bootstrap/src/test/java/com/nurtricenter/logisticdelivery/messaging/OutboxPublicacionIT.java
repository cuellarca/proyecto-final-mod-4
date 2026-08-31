package com.nurtricenter.logisticdelivery.messaging;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import com.nurtricenter.logisticdelivery.infrastructure.messaging.NotificacionesStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaRepository;
import com.nurtricenter.logisticdelivery.support.PostgisContainerConfig;
import com.nurtricenter.logisticdelivery.support.RabbitContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Fase 4 — un evento escrito en el {@code outbox} se publica (publicador @Scheduled -> RabbitMQ) y
 * llega al consumidor stub. Usa Testcontainers para Postgres+PostGIS y RabbitMQ. Requiere un entorno
 * Docker accesible por la libreria de Testcontainers.
 */
@SpringBootTest(classes = LogisticDeliveryApplication.class, properties = "logistic.outbox.poll-delay-ms=500")
@Import({PostgisContainerConfig.class, RabbitContainerConfig.class})
class OutboxPublicacionIT {

    @Autowired
    OutboxJpaRepository outbox;
    @Autowired
    NotificacionesStubListener notificaciones;

    @Test
    void unEventoDelOutboxSePublicaYLlegaAlConsumidor() {
        String paciente = "pac-tc-1";
        outbox.save(new OutboxJpaEntity(
                UUID.randomUUID(),
                "EntregaConfirmada",
                UUID.randomUUID().toString(),
                "{\"pacienteId\":\"" + paciente + "\"}",
                Instant.parse("2026-07-10T12:00:00Z")));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificaciones.recibidos())
                        .anySatisfy(e -> {
                            assertThat(e.type()).isEqualTo("EntregaConfirmada");
                            assertThat(e.payload().path("pacienteId").asText()).isEqualTo(paciente);
                        }));
    }
}
