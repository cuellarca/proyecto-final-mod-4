package com.nurtricenter.logisticdelivery.application.usecase.query;

import java.time.LocalDate;
import java.util.List;

/** Vista de lectura de una ruta del repartidor y sus paradas (HU-1). */
public record RutaView(
        String rutaId,
        String repartidorId,
        LocalDate fecha,
        String estado,
        List<ParadaView> paradas) {

    public record ParadaView(
            String paradaId,
            String paqueteId,
            String pacienteId,
            int orden,
            double lat,
            double lon,
            String estado) {
    }
}
