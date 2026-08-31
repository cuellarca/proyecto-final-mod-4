package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;

/** Comando de entrada de {@link RegistrarEntregaFallida} (HU-5): la entrega y el motivo del fallo. */
public record RegistrarEntregaFallidaCommand(String entregaId, MotivoFallo motivo) {
}
