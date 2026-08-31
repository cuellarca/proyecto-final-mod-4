package com.nurtricenter.logisticdelivery.domain.repository;

import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio del agregado {@code RutaDeEntrega} (DDD). Es una interfaz
 * pura declarada en el dominio: solo referencia tipos del dominio, sin framework.
 * La implementacion (adaptador JPA/PostGIS) vive en infraestructura.
 */
public interface RutaRepository {

    /** Persiste el agregado completo (raiz + paradas) en la misma transaccion del caso de uso. */
    void guardar(RutaDeEntrega ruta);

    /** Rehidrata el agregado a partir de su identidad. */
    Optional<RutaDeEntrega> buscarPorId(RutaId id);

    /** Rutas de un repartidor en un dia (consulta de operacion, HU-1). */
    List<RutaDeEntrega> buscarPorRepartidorYFecha(RepartidorId repartidorId, Fecha fecha);
}
