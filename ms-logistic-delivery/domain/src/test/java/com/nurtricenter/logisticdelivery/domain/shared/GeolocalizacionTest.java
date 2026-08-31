package com.nurtricenter.logisticdelivery.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeolocalizacionTest {

    @Test
    @DisplayName("Rechaza latitud fuera del rango [-90, 90]")
    void rechazaLatitudFueraDeRango() {
        assertThrows(DomainException.class, () -> new Geolocalizacion(91.0, 0.0));
    }

    @Test
    @DisplayName("Rechaza longitud fuera del rango [-180, 180]")
    void rechazaLongitudFueraDeRango() {
        assertThrows(DomainException.class, () -> new Geolocalizacion(0.0, 200.0));
    }

    @Test
    @DisplayName("Haversine entre Santa Cruz y La Paz ronda los 550 km")
    void calculaDistanciaHaversineAproximada() {
        Geolocalizacion santaCruz = new Geolocalizacion(-17.7833, -63.1821);
        Geolocalizacion laPaz = new Geolocalizacion(-16.5000, -68.1500);
        double distancia = santaCruz.distanciaHaversineKm(laPaz);
        assertTrue(distancia > 500 && distancia < 600, "distancia esperada ~550 km, fue " + distancia);
    }

    @Test
    @DisplayName("La distancia de un punto a si mismo es cero")
    void distanciaAlMismoPuntoEsCero() {
        Geolocalizacion g = new Geolocalizacion(-17.78, -63.18);
        assertEquals(0.0, g.distanciaHaversineKm(g), 0.0001);
    }
}
