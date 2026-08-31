package com.nurtricenter.logisticdelivery.persistence;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import com.nurtricenter.logisticdelivery.support.PostgisContainerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Fase 1 — round-trip de persistencia contra un PostGIS efimero levantado por Testcontainers
 * (imagen {@code postgis/postgis}). Requiere un entorno Docker accesible por la libreria de
 * Testcontainers (Docker Engine con API >= 1.44).
 */
@SpringBootTest(classes = LogisticDeliveryApplication.class)
@Import(PostgisContainerConfig.class)
class PersistenciaRoundTripIT extends PersistenciaRoundTripTests {
}
