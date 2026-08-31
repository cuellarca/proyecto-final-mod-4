package com.nurtricenter.logisticdelivery.domain.ruta;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParadaTest {

    private Geolocalizacion geo() {
        return new Geolocalizacion(-17.78, -63.18);
    }

    @Test
    @DisplayName("HU-2: no se puede crear una parada sin geolocalizacion")
    void noSePuedeCrearSinGeolocalizacion() {
        assertThrows(NullPointerException.class,
                () -> Parada.crear(PaqueteId.de("P1"), PacienteId.de("PA1"), 1, null));
    }

    @Test
    @DisplayName("El orden de la parada debe ser positivo")
    void ordenDebeSerPositivo() {
        assertThrows(DomainException.class,
                () -> Parada.crear(PaqueteId.de("P1"), PacienteId.de("PA1"), 0, geo()));
    }

    @Test
    @DisplayName("Transiciones validas: PENDIENTE -> EN_CURSO -> COMPLETADA")
    void transicionesValidas() {
        Parada parada = Parada.crear(PaqueteId.de("P1"), PacienteId.de("PA1"), 1, geo());
        assertEquals(EstadoParada.PENDIENTE, parada.estado());
        parada.iniciar();
        assertEquals(EstadoParada.EN_CURSO, parada.estado());
        parada.completar();
        assertEquals(EstadoParada.COMPLETADA, parada.estado());
    }

    @Test
    @DisplayName("No se puede completar una parada que no fue iniciada")
    void noPuedeCompletarSinIniciar() {
        Parada parada = Parada.crear(PaqueteId.de("P1"), PacienteId.de("PA1"), 1, geo());
        assertThrows(DomainException.class, parada::completar);
    }
}
