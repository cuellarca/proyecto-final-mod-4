package com.nurtricenter.logisticdelivery.application.usecase.query;

import java.time.Instant;

/** Vista de lectura de la constancia de una entrega (HU-6). {@code urlEvidencia} apunta a la foto/firma. */
public record ConstanciaView(
        String entregaId,
        Instant timestamp,
        double lat,
        double lon,
        String urlEvidencia,
        String nombreReceptor) {
}
