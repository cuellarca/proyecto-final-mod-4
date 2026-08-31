package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Fase 6 — flujo end-to-end de las 6 HUs contra PostGIS + RabbitMQ externos ya en ejecucion
 * (Postgres en 5435, RabbitMQ en 5672). Alternativa a Testcontainers cuando el entorno no expone el
 * socket Docker a la libreria Java. Se activa con -Dmsld.external.db=true.
 */
@EnabledIfSystemProperty(named = "msld.external.db", matches = "true")
@SpringBootTest(
        classes = LogisticDeliveryApplication.class,
        properties = {
                "logistic.reintentos.maximo=1",
                "logistic.outbox.poll-delay-ms=500"
        })
@AutoConfigureMockMvc
class Fase6E2EExternaManualIT extends Fase6E2EFlujoTests {
}
