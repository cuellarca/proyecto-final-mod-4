package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;

/**
 * Par de coordenadas (Value Object) compartido con la {@code DireccionDeEntrega}
 * del BC de Suscripcion. Se define por su valor, es inmutable y sabe calcular la
 * distancia geografica a otra coordenada (formula de Haversine).
 */
public record Geolocalizacion(double lat, double lon) {

    private static final double RADIO_TIERRA_KM = 6371.0088;

    public Geolocalizacion {
        if (lat < -90.0 || lat > 90.0) {
            throw new DomainException("Latitud fuera de rango [-90, 90]: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new DomainException("Longitud fuera de rango [-180, 180]: " + lon);
        }
    }

    /** Distancia en kilometros a otra geolocalizacion por la formula de Haversine. */
    public double distanciaHaversineKm(Geolocalizacion otra) {
        Objects.requireNonNull(otra, "La otra geolocalizacion no puede ser nula");
        double dLat = Math.toRadians(otra.lat - this.lat);
        double dLon = Math.toRadians(otra.lon - this.lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(otra.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
