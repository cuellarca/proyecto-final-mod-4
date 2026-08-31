package com.nurtricenter.logisticdelivery.domain.ruta;

import com.nurtricenter.logisticdelivery.domain.ruta.event.RutaDeEntregaPlanificada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorPorCercania;
import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.ParadaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutaDeEntregaTest {

    private Parada parada(int orden, double lat, double lon) {
        return Parada.crear(PaqueteId.de("P" + orden), PacienteId.de("PA" + orden), orden,
                new Geolocalizacion(lat, lon));
    }

    private RutaDeEntrega rutaConDosParadas() {
        return RutaDeEntrega.planificar(RepartidorId.de("REP-1"), Fecha.de(2026, 7, 1),
                List.of(parada(1, -17.78, -63.18), parada(2, -17.79, -63.19)));
    }

    @Test
    @DisplayName("Planificar valida las invariantes y emite el evento")
    void planificarValidaYEmiteEvento() {
        RutaDeEntrega ruta = rutaConDosParadas();
        assertEquals(EstadoRuta.PLANIFICADA, ruta.estado());
        assertTrue(ruta.pullDomainEvents().stream().anyMatch(e -> e instanceof RutaDeEntregaPlanificada));
    }

    @Test
    @DisplayName("Rechaza un orden de paradas no contiguo (1, 3)")
    void rechazaOrdenNoContiguo() {
        assertThrows(DomainException.class, () -> RutaDeEntrega.planificar(RepartidorId.de("R1"),
                Fecha.de(2026, 7, 1), List.of(parada(1, -17.78, -63.18), parada(3, -17.79, -63.19))));
    }

    @Test
    @DisplayName("Rechaza un orden de paradas duplicado (1, 1)")
    void rechazaOrdenDuplicado() {
        assertThrows(DomainException.class, () -> RutaDeEntrega.planificar(RepartidorId.de("R1"),
                Fecha.de(2026, 7, 1), List.of(parada(1, -17.78, -63.18), parada(1, -17.79, -63.19))));
    }

    @Test
    @DisplayName("Una ruta no puede planificarse sin paradas")
    void rechazaRutaVacia() {
        assertThrows(DomainException.class, () -> RutaDeEntrega.planificar(RepartidorId.de("R1"),
                Fecha.de(2026, 7, 1), List.of()));
    }

    @Test
    @DisplayName("El flujo de ejecucion finaliza la ruta cuando todas las paradas son terminales")
    void flujoDeEjecucionFinalizaRuta() {
        RutaDeEntrega ruta = rutaConDosParadas();
        ruta.iniciar();
        List<ParadaId> ids = ruta.paradas().stream().map(Parada::id).toList();

        ruta.avanzarParada(ids.get(0));
        ruta.completarParada(ids.get(0));
        ruta.avanzarParada(ids.get(1));
        ruta.fallarParada(ids.get(1));

        assertEquals(EstadoRuta.FINALIZADA, ruta.estado());
    }

    @Test
    @DisplayName("No se pueden operar paradas antes de iniciar la ruta")
    void noSePuedenOperarParadasAntesDeIniciar() {
        RutaDeEntrega ruta = RutaDeEntrega.planificar(RepartidorId.de("R1"), Fecha.de(2026, 7, 1),
                List.of(parada(1, -17.78, -63.18)));
        ParadaId id = ruta.paradas().get(0).id();
        assertThrows(DomainException.class, () -> ruta.avanzarParada(id));
    }

    @Test
    @DisplayName("HU-3: la optimizacion reordena las paradas por cercania y reasigna el orden")
    void optimizacionReordenaPorCercania() {
        Parada lejana = parada(1, -17.80, -63.20);   // lejos del origen
        Parada cercana = parada(2, -17.781, -63.181); // pegada al origen
        RutaDeEntrega ruta = RutaDeEntrega.planificar(RepartidorId.de("R1"), Fecha.de(2026, 7, 1),
                List.of(lejana, cercana));

        ruta.aplicarOptimizacion(new OptimizadorPorCercania(), new Geolocalizacion(-17.78, -63.18));

        assertEquals(cercana.id(), ruta.paradas().get(0).id());
        assertEquals(1, ruta.paradas().get(0).orden());
        assertEquals(2, ruta.paradas().get(1).orden());
    }
}
