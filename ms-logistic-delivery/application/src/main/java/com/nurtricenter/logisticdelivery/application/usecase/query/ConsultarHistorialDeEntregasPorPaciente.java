package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.application.port.out.HistorialReadModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Query CQRS (HU-6): historial de entregas de un paciente en un rango de fechas, leido del read model.
 */
@Service
public class ConsultarHistorialDeEntregasPorPaciente {

    private static final Instant INICIO_ABIERTO = Instant.EPOCH;
    private static final Instant FIN_ABIERTO = Instant.parse("2999-12-31T23:59:59Z");

    private final HistorialReadModel readModel;

    public ConsultarHistorialDeEntregasPorPaciente(HistorialReadModel readModel) {
        this.readModel = readModel;
    }

    public List<HistorialEntregaView> ejecutar(String pacienteId, LocalDate desde, LocalDate hasta) {
        Instant inicio = desde != null ? desde.atStartOfDay(ZoneOffset.UTC).toInstant() : INICIO_ABIERTO;
        Instant fin = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : FIN_ABIERTO;
        return readModel.historialDePaciente(pacienteId, inicio, fin);
    }
}
