package com.nurtricenter.logisticdelivery.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /api/v1/entregas/{id}/confirmacion} (HU-4): la evidencia de la entrega. */
public record ConfirmarEntregaRequest(
        @NotNull Double lat,
        @NotNull Double lon,
        @NotBlank String urlEvidencia,
        @NotBlank String nombreReceptor) {
}
