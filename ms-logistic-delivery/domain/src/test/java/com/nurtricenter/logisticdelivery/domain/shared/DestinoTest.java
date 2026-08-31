package com.nurtricenter.logisticdelivery.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-2: {@code Destino} es la entrada del geocodificador. Empieza sin coordenadas y se enriquece
 * cuando el proveedor las resuelve, sin mutar el value object original.
 */
class DestinoTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("HU-2: un destino sin texto util no es un destino")
    void rechazaTextoVacio(String texto) {
        assertThrows(DomainException.class, () -> Destino.sinGeocodificar(texto));
    }

    @Test
    @DisplayName("HU-2: un destino recien creado esta pendiente de geocodificar")
    void sinGeocodificarNoTieneCoordenadas() {
        Destino destino = Destino.sinGeocodificar("Av. Banzer 3er anillo");

        assertFalse(destino.estaGeocodificado());
    }

    @Test
    @DisplayName("HU-2: geocodificar produce una copia; el destino original no cambia")
    void conGeolocalizacionNoMutaElOriginal() {
        Destino original = Destino.sinGeocodificar("Av. Banzer 3er anillo");

        original.conGeolocalizacion(new Geolocalizacion(-17.78, -63.18));

        assertFalse(original.estaGeocodificado());
    }

    @Test
    @DisplayName("HU-2: el destino geocodificado expone las coordenadas resueltas")
    void geocodificadoExponeSusCoordenadas() {
        Destino destino = Destino.geocodificado("Av. Banzer", new Geolocalizacion(-17.78, -63.18));

        assertEquals(-17.78, destino.geo().orElseThrow().lat());
    }

    @Test
    @DisplayName("HU-2: un destino ya geocodificado se reconoce como tal")
    void geocodificadoEstaGeocodificado() {
        Destino destino = Destino.geocodificado("Av. Banzer", new Geolocalizacion(-17.78, -63.18));

        assertTrue(destino.estaGeocodificado());
    }
}
