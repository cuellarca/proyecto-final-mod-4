package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import com.nurtricenter.logisticdelivery.support.PostgisContainerConfig;
import com.nurtricenter.logisticdelivery.support.RabbitContainerConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Fase 6 — flujo end-to-end de las 6 HUs con Testcontainers (Postgres+PostGIS y RabbitMQ). Es el test
 * de integracion canonico del proyecto; requiere un entorno Docker accesible por Testcontainers.
 */
@SpringBootTest(
        classes = LogisticDeliveryApplication.class,
        properties = {
                "logistic.reintentos.maximo=1",
                "logistic.outbox.poll-delay-ms=500"
        })
@Import({PostgisContainerConfig.class, RabbitContainerConfig.class})
@AutoConfigureMockMvc
class FlujoCompletoE2EIT extends Fase6E2EFlujoTests {
}
