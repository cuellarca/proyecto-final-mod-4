package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion de dominio del optimizador (fallback de HU-3): heuristica del
 * vecino mas cercano usando la distancia Haversine. Es pura y testeable: no
 * depende de ningun proveedor externo, por lo que sirve como degradacion cuando
 * el proveedor de mapas no responde.
 */
public class OptimizadorPorCercania implements OptimizadorDeRutas {

    @Override
    public List<Parada> optimizar(Geolocalizacion origen, List<Parada> paradas) {
        List<Parada> pendientes = new ArrayList<>(paradas);
        List<Parada> secuencia = new ArrayList<>(paradas.size());
        Geolocalizacion actual = origen;

        while (!pendientes.isEmpty()) {
            Parada masCercana = null;
            double mejorDistancia = Double.MAX_VALUE;
            for (Parada candidata : pendientes) {
                double distancia = actual.distanciaHaversineKm(candidata.geolocalizacion());
                if (distancia < mejorDistancia) {
                    mejorDistancia = distancia;
                    masCercana = candidata;
                }
            }
            secuencia.add(masCercana);
            pendientes.remove(masCercana);
            actual = masCercana.geolocalizacion();
        }
        return secuencia;
    }
}
