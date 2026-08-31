package com.nurtricenter.logisticdelivery.application.usecase.command;

/**
 * Comando de entrada de {@link ConfirmarEntrega} (HU-4): la evidencia de la entrega exitosa.
 * La {@code Url} apunta a la foto/firma en el object storage (simulado).
 */
public record ConfirmarEntregaCommand(
        String entregaId,
        double lat,
        double lon,
        String urlEvidencia,
        String nombreReceptor) {
}
