package com.nurtricenter.logisticdelivery.infrastructure.rest.dto;

import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /api/v1/entregas/{id}/fallo} (HU-5): el motivo obligatorio del fallo. */
public record RegistrarFalloRequest(@NotNull MotivoFallo motivo) {
}
