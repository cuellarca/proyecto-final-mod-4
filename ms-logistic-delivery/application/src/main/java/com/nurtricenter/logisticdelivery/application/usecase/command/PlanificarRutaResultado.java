package com.nurtricenter.logisticdelivery.application.usecase.command;

import java.util.List;

/**
 * Resultado de {@link PlanificarRutaDelDia}: la ruta creada y las entregas programadas (una por
 * paquete). Los ids de entrega permiten confirmar/fallar cada entrega despues (HU-4/HU-5).
 */
public record PlanificarRutaResultado(String rutaId, List<EntregaProgramada> entregas) {

    /** Vinculo entre un paquete y la {@code Entrega} que se creo para el. */
    public record EntregaProgramada(String entregaId, String paqueteId, String pacienteId) {
    }
}
