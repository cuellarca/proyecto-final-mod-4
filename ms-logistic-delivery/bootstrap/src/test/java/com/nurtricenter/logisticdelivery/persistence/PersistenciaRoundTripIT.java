package com.nurtricenter.logisticdelivery.persistence;

import com.nurtricenter.logisticdelivery.support.PruebaDeIntegracion;

/**
 * Fase 1 — round-trip de persistencia contra un PostGIS efimero levantado por Testcontainers
 * (imagen {@code postgis/postgis}). Requiere un entorno Docker accesible por la libreria de
 * Testcontainers (Docker Engine con API >= 1.44).
 */
@PruebaDeIntegracion
class PersistenciaRoundTripIT extends PersistenciaRoundTripTests {
}
