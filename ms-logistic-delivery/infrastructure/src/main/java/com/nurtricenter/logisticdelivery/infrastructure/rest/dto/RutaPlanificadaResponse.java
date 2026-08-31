package com.nurtricenter.logisticdelivery.infrastructure.rest.dto;

import java.util.List;

/** Respuesta de {@code POST /api/v1/rutas}: la ruta creada y las entregas programadas. */
public record RutaPlanificadaResponse(String rutaId, List<EntregaProgramadaResponse> entregas) {

    public record EntregaProgramadaResponse(String entregaId, String paqueteId, String pacienteId) {
    }
}
