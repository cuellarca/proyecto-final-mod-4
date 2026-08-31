package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.domain.service.Geocodificador;
import com.nurtricenter.logisticdelivery.domain.shared.Destino;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.springframework.stereotype.Service;

/**
 * OHS (Open Host Service) HU-2: resuelve un texto de direccion a coordenadas usando el puerto
 * {@code Geocodificador}. Es una operacion de consulta (no muta agregados ni emite eventos); la
 * expone Logistica para el BC de Suscripcion.
 */
@Service
public class GeocodificarDireccion {

    private final Geocodificador geocodificador;

    public GeocodificarDireccion(Geocodificador geocodificador) {
        this.geocodificador = geocodificador;
    }

    public Geolocalizacion ejecutar(String textoDireccion) {
        return geocodificador.geocodificar(Destino.sinGeocodificar(textoDireccion));
    }
}
