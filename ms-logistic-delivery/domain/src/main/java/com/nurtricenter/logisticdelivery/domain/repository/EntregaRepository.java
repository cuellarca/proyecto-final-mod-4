package com.nurtricenter.logisticdelivery.domain.repository;

import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio del agregado {@code Entrega} (DDD). Interfaz pura del dominio;
 * la implementacion (adaptador JPA/PostGIS) vive en infraestructura.
 */
public interface EntregaRepository {

    /** Persiste el agregado en la misma transaccion del caso de uso. */
    void guardar(Entrega entrega);

    /** Rehidrata la entrega a partir de su identidad. */
    Optional<Entrega> buscarPorId(EntregaId id);

    /** Entregas programadas dentro de una ruta (consulta de operacion). */
    List<Entrega> buscarPorRuta(RutaId rutaId);
}
