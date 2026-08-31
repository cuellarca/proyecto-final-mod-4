package com.nurtricenter.logisticdelivery.persistence;

import com.nurtricenter.logisticdelivery.LogisticDeliveryApplication;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verificacion del round-trip contra un PostGIS externo ya en ejecucion (p. ej. el de docker-compose),
 * como alternativa a Testcontainers cuando el entorno no expone el socket Docker a la libreria Java.
 * Se activa solo con -Dmsld.external.db=true para no correr en un build normal.
 */
@EnabledIfSystemProperty(named = "msld.external.db", matches = "true")
@SpringBootTest(classes = LogisticDeliveryApplication.class)
class PersistenciaRoundTripExternaManualIT extends PersistenciaRoundTripTests {
}
