package com.nurtricenter.logisticdelivery.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Cuerpo de {@code POST /api/v1/rutas}: la carga de entregas del dia a planificar (HU-1/HU-3). */
public record PlanificarRutaRequest(
        @NotBlank String repartidorId,
        @NotNull LocalDate fecha,
        @NotNull Double origenLat,
        @NotNull Double origenLon,
        @NotEmpty @Valid List<PaqueteRequest> paquetes) {

    /** Un paquete a entregar, con sus coordenadas ya resueltas por Produccion. */
    public record PaqueteRequest(
            @NotBlank String paqueteId,
            @NotBlank String pacienteId,
            @NotNull Double lat,
            @NotNull Double lon) {
    }
}
