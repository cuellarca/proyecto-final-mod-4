package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptimizadorPorCercaniaTest {

    @Test
    @DisplayName("Ordena las paradas por el vecino mas cercano desde el origen")
    void ordenaPorVecinoMasCercano() {
        Parada lejos = Parada.crear(PaqueteId.de("A"), PacienteId.de("A"), 1,
                new Geolocalizacion(-17.90, -63.30));
        Parada cerca = Parada.crear(PaqueteId.de("B"), PacienteId.de("B"), 2,
                new Geolocalizacion(-17.781, -63.181));

        List<Parada> ordenadas = new OptimizadorPorCercania()
                .optimizar(new Geolocalizacion(-17.78, -63.18), List.of(lejos, cerca));

        assertEquals(cerca.id(), ordenadas.get(0).id());
        assertEquals(lejos.id(), ordenadas.get(1).id());
    }
}
