package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;

import java.util.List;

/**
 * Domain Service (puerto): calcula la secuencia optima de recorrido de las paradas
 * geolocalizadas (HU-3). La implementacion "buena" vive en infraestructura
 * (ACL al proveedor de mapas); el dominio ofrece {@code OptimizadorPorCercania}
 * como fallback puro cuando el proveedor no responde.
 */
public interface OptimizadorDeRutas {

    /** Devuelve las paradas en la secuencia optima a recorrer desde un origen. */
    List<Parada> optimizar(Geolocalizacion origen, List<Parada> paradas);
}
