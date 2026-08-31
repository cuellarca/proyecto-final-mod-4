package com.nurtricenter.logisticdelivery.application.usecase.command;

/** Comando de entrada de {@link ReintentarEntrega} (HU-5): la entrega fallida a reintentar. */
public record ReintentarEntregaCommand(String entregaId) {
}
