package com.nurtricenter.logisticdelivery.application.usecase.command;

import java.time.LocalDate;
import java.util.List;

/**
 * Comando de entrada de {@link PlanificarRutaDelDia}: el repartidor, el dia, el origen del recorrido
 * y los paquetes a entregar (ya geolocalizados por el productor externo).
 */
public record PlanificarRutaDelDiaCommand(
        String repartidorId,
        LocalDate fecha,
        double origenLat,
        double origenLon,
        List<Paquete> paquetes) {

    /** Un paquete a entregar con sus coordenadas ya resueltas. */
    public record Paquete(String paqueteId, String pacienteId, double lat, double lon) {
    }
}
