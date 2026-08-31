package com.nurtricenter.logisticdelivery.application.port.out;

import com.nurtricenter.logisticdelivery.application.usecase.query.ConstanciaView;
import com.nurtricenter.logisticdelivery.application.usecase.query.HistorialEntregaView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de lectura: consulta el read model del historial de entregas (poblado por el
 * proyector). Las consultas leen solo de aqui, sin tocar el lado de escritura (CQRS, HU-6).
 */
public interface HistorialReadModel {

    /** Entregas de un paciente cuyo ultimo evento cae en el rango [desde, hasta]. */
    List<HistorialEntregaView> historialDePaciente(String pacienteId, Instant desde, Instant hasta);

    /** Constancia de una entrega, si ya fue confirmada. */
    Optional<ConstanciaView> constanciaDe(String entregaId);
}
