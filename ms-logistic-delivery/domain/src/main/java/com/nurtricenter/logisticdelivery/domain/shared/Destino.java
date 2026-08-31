package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;
import java.util.Optional;

/**
 * Lugar de entrega descrito por texto y, opcionalmente, sus coordenadas (Value Object).
 * Es la entrada del {@code Geocodificador}: puede empezar sin geolocalizacion y
 * enriquecerse una vez resuelta (HU-2).
 */
public record Destino(String texto, Geolocalizacion geolocalizacion) {

    public Destino {
        Objects.requireNonNull(texto, "El destino requiere un texto");
        if (texto.isBlank()) {
            throw new DomainException("El destino no puede estar vacio");
        }
    }

    /** Destino aun sin geocodificar (pendiente de resolver a coordenadas). */
    public static Destino sinGeocodificar(String texto) {
        return new Destino(texto, null);
    }

    /** Destino con coordenadas ya resueltas. */
    public static Destino geocodificado(String texto, Geolocalizacion geo) {
        return new Destino(texto, Objects.requireNonNull(geo, "Geolocalizacion requerida"));
    }

    public boolean estaGeocodificado() {
        return geolocalizacion != null;
    }

    public Optional<Geolocalizacion> geo() {
        return Optional.ofNullable(geolocalizacion);
    }

    /** Devuelve una copia del destino ya geocodificado (inmutabilidad del VO). */
    public Destino conGeolocalizacion(Geolocalizacion geo) {
        return new Destino(texto, Objects.requireNonNull(geo, "Geolocalizacion requerida"));
    }
}
