package com.nurtricenter.logisticdelivery.flujo;

import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;

/**
 * Fase 6 — flujo end-to-end de las 6 HUs con Testcontainers (Postgres+PostGIS y RabbitMQ). Es el test
 * de integracion canonico del proyecto; requiere un entorno Docker accesible por Testcontainers.
 */
@PruebaDeIntegracion
class FlujoCompletoE2EIT extends Fase6E2EFlujoTests {
}
