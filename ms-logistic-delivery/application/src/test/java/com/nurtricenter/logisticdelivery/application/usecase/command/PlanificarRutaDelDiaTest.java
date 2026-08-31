package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.repository.RutaRepository;
import com.nurtricenter.logisticdelivery.domain.ruta.EstadoRuta;
import com.nurtricenter.logisticdelivery.domain.ruta.RutaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.ruta.event.RutaDeEntregaPlanificada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorDeRutas;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificarRutaDelDiaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    RutaRepository rutaRepository;
    @Mock
    EntregaRepository entregaRepository;
    @Mock
    OptimizadorDeRutas optimizador;
    @Mock
    PublicadorDeEventos publicador;
    @Captor
    ArgumentCaptor<RutaDeEntrega> rutaCaptor;
    @Captor
    ArgumentCaptor<Entrega> entregaCaptor;
    @Captor
    ArgumentCaptor<List<DomainEvent>> eventosCaptor;

    @Test
    void planificaOptimizaProgramaEntregasYPublicaElEvento() {
        PlanificarRutaDelDia useCase = new PlanificarRutaDelDia(
                rutaRepository, entregaRepository, optimizador, publicador, RELOJ, 2);
        // El optimizador (mock) devuelve las mismas paradas que recibe (secuencia valida).
        when(optimizador.optimizar(any(Geolocalizacion.class), anyList())).thenAnswer(inv -> inv.getArgument(1));

        PlanificarRutaDelDiaCommand comando = new PlanificarRutaDelDiaCommand(
                "rep-1", LocalDate.of(2026, 7, 10), -17.78, -63.18,
                List.of(
                        new PlanificarRutaDelDiaCommand.Paquete("p1", "pac1", -17.79, -63.19),
                        new PlanificarRutaDelDiaCommand.Paquete("p2", "pac2", -17.80, -63.20)));

        PlanificarRutaResultado resultado = useCase.ejecutar(comando);

        assertThat(resultado.rutaId()).isNotBlank();
        assertThat(resultado.entregas()).hasSize(2);
        verify(optimizador).optimizar(any(Geolocalizacion.class), anyList());
        verify(rutaRepository).guardar(rutaCaptor.capture());
        RutaDeEntrega guardada = rutaCaptor.getValue();
        assertThat(guardada.estado()).isEqualTo(EstadoRuta.PLANIFICADA);
        assertThat(guardada.paradas()).hasSize(2);

        // Se programa una entrega por paquete (HU-1).
        verify(entregaRepository, org.mockito.Mockito.times(2)).guardar(entregaCaptor.capture());

        verify(publicador).publicar(eventosCaptor.capture());
        assertThat(eventosCaptor.getValue())
                .anySatisfy(e -> assertThat(e).isInstanceOf(RutaDeEntregaPlanificada.class));
    }
}
