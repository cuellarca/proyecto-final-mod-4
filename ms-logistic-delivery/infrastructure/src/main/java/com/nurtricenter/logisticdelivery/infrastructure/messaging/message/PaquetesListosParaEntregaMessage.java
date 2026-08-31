package com.nurtricenter.logisticdelivery.infrastructure.messaging.message;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato del mensaje de entrada {@code PaquetesListosParaEntrega} que emite el BC de Produccion
 * (HU-1). Los paquetes llegan <b>ya geolocalizados</b>.
 */
public record PaquetesListosParaEntregaMessage(
        String repartidorId,
        LocalDate fecha,
        double origenLat,
        double origenLon,
        List<PaqueteMsg> paquetes) {

    public record PaqueteMsg(String paqueteId, String pacienteId, double lat, double lon) {
    }
}
