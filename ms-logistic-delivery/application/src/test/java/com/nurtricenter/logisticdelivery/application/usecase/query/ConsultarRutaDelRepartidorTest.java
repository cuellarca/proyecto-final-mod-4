package com.nurtricenter.logisticdelivery.application.usecase.query;

import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.shared.Fecha;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RepartidorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * HU-1: la ruta del repartidor se lee del lado de escritura y se proyecta a una vista. El seam es
 * {@code RutaRepository}; el agregado {@code RutaDeEntrega} y sus paradas se construyen de verdad
 * (testing-rules.md, LIMITES: el dominio nunca se mockea).
 */
@ExtendWith(MockitoExtension.class)
class ConsultarRutaDelRepartidorTest {

    private static final LocalDate DIA = LocalDate.parse("2026-07-10");

    @Mock
    RutaRepository rutaRepository;

    private RutaDeEntrega rutaConDosParadas() {
        return RutaDeEntrega.planificar(
                RepartidorId.de("rep-1"), Fecha.de(DIA),
                List.of(
                        Parada.crear(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), 1,
                                new Geolocalizacion(-17.78, -63.18)),
                        Parada.crear(PaqueteId.de("pkg-2"), PacienteId.de("pac-2"), 2,
                                new Geolocalizacion(-17.79, -63.19))));
    }

    @Test
    @DisplayName("HU-1: cada parada del agregado se proyecta con su orden y coordenadas")
    void proyectaLasParadasDeLaRuta() {
        when(rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de("rep-1"), Fecha.de(DIA)))
                .thenReturn(List.of(rutaConDosParadas()));
        ConsultarRutaDelRepartidor query = new ConsultarRutaDelRepartidor(rutaRepository);

        List<RutaView> rutas = query.ejecutar("rep-1", DIA);

        assertThat(rutas).singleElement()
                .satisfies(ruta -> assertThat(ruta.paradas())
                        .extracting(RutaView.ParadaView::paqueteId, RutaView.ParadaView::orden,
                                RutaView.ParadaView::lat, RutaView.ParadaView::estado)
                        .containsExactly(
                                org.assertj.core.groups.Tuple.tuple("pkg-1", 1, -17.78, "PENDIENTE"),
                                org.assertj.core.groups.Tuple.tuple("pkg-2", 2, -17.79, "PENDIENTE")));
    }

    @Test
    @DisplayName("HU-1: un repartidor sin rutas ese dia devuelve lista vacia")
    void devuelveVacioCuandoNoHayRutas() {
        when(rutaRepository.buscarPorRepartidorYFecha(RepartidorId.de("rep-9"), Fecha.de(DIA)))
                .thenReturn(List.of());
        ConsultarRutaDelRepartidor query = new ConsultarRutaDelRepartidor(rutaRepository);

        List<RutaView> rutas = query.ejecutar("rep-9", DIA);

        assertThat(rutas).isEmpty();
    }
}
