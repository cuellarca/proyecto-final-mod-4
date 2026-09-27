package com.nurtricenter.logisticdelivery.messaging;

import com.nurtricenter.logisticdelivery.infrastructure.messaging.NotificacionesStubListener;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaEntity;
import com.nurtricenter.logisticdelivery.infrastructure.outbox.OutboxJpaRepository;
import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
@Tag("flujo-a")
@PruebaDeIntegracion
class OutboxPublicacionIT {

    @Autowired
    OutboxJpaRepository outbox;
    @Autowired
    NotificacionesStubListener notificaciones;

    @Test
    @DisplayName("HU-4 · flujo correcto: un evento del Outbox se publica y llega al consumidor")
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
