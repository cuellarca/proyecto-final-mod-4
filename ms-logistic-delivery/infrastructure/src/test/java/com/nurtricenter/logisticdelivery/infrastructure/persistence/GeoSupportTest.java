package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Traduccion entre la {@code Geolocalizacion} del dominio y el {@code Point} de PostGIS. El error
 * clasico de este borde es invertir los ejes: PostGIS usa X = longitud, Y = latitud. Invertirlos no
 * rompe nada al compilar, solo pone las entregas en otro continente.
 */
class GeoSupportTest {

    private static final Geolocalizacion SANTA_CRUZ = new Geolocalizacion(-17.7833, -63.1821);

    @Test
    @DisplayName("La longitud va en el eje X (convencion PostGIS/GeoJSON)")
    void laLongitudVaEnElEjeX() {
        Point punto = GeoSupport.toPoint(SANTA_CRUZ);

        assertThat(punto.getX()).isEqualTo(-63.1821);
    }

    @Test
    @DisplayName("La latitud va en el eje Y (convencion PostGIS/GeoJSON)")
    void laLatitudVaEnElEjeY() {
        Point punto = GeoSupport.toPoint(SANTA_CRUZ);

        assertThat(punto.getY()).isEqualTo(-17.7833);
    }

    @Test
    @DisplayName("El punto se persiste en WGS-84 (SRID 4326), como declara la migracion")
    void elPuntoLlevaElSridWgs84() {
        Point punto = GeoSupport.toPoint(SANTA_CRUZ);

        assertThat(punto.getSRID()).isEqualTo(4326);
    }

    @Test
    @DisplayName("Ida y vuelta conserva las coordenadas: guardar y leer no mueve la entrega")
    void laIdaYVueltaConservaLasCoordenadas() {
        Geolocalizacion recuperada = GeoSupport.toGeolocalizacion(GeoSupport.toPoint(SANTA_CRUZ));

        assertThat(recuperada).isEqualTo(SANTA_CRUZ);
    }
}
