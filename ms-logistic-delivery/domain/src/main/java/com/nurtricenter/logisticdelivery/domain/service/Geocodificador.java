package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.shared.Destino;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;

/**
 * Domain Service (puerto): convierte un {@code Destino} (texto) en coordenadas (HU-2).
 * Logistica es el dueno del conocimiento geografico; la implementacion concreta
 * vive en infraestructura como ACL al proveedor de mapas.
 */
public interface Geocodificador {

    /** Resuelve las coordenadas de un destino descrito por texto. */
    Geolocalizacion geocodificar(Destino destino);
}
