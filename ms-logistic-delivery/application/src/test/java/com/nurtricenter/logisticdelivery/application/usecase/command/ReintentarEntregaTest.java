package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.Intentos;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaNoConcretada;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaReprogramada;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.service.PoliticaDeReintento;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import com.nurtricenter.logisticdelivery.domain.shared.RutaId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReintentarEntregaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    EntregaRepository entregaRepository;
    @Mock
    PublicadorDeEventos publicador;
    @Captor
    ArgumentCaptor<List<DomainEvent>> eventosCaptor;

    private final PoliticaDeReintento politica = new PoliticaDeReintento();

    @Test
    void reprogramaCuandoQuedaSaldoDeIntentos() {
        Entrega entrega = Entrega.programar(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), RutaId.nuevo(), 2);
        entrega.registrarFallo(MotivoFallo.AUSENTE);
        entrega.pullDomainEvents(); // descarta el EntregaFallida previo
        when(entregaRepository.buscarPorId(entrega.id())).thenReturn(Optional.of(entrega));
        ReintentarEntrega useCase = new ReintentarEntrega(entregaRepository, politica, publicador, RELOJ);

        useCase.ejecutar(new ReintentarEntregaCommand(entrega.id().toString()));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(entrega.intentos().valor()).isEqualTo(1);
        verify(publicador).publicar(eventosCaptor.capture());
        assertThat(eventosCaptor.getValue())
                .anySatisfy(e -> assertThat(e).isInstanceOf(EntregaReprogramada.class));
    }

    @Test
    void marcaNoConcretadaCuandoSeAgotanLosIntentos() {
        // Entrega FALLIDA con los intentos ya agotados (valor == maximo).
        Entrega entrega = Entrega.reconstituir(
                EntregaId.nuevo(), PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), RutaId.nuevo(),
                EstadoEntrega.FALLIDA, new Intentos(1, 1), MotivoFallo.AUSENTE, null);
        when(entregaRepository.buscarPorId(entrega.id())).thenReturn(Optional.of(entrega));
        ReintentarEntrega useCase = new ReintentarEntrega(entregaRepository, politica, publicador, RELOJ);

        useCase.ejecutar(new ReintentarEntregaCommand(entrega.id().toString()));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.NO_CONCRETADA);
        verify(publicador).publicar(eventosCaptor.capture());
        assertThat(eventosCaptor.getValue())
                .anySatisfy(e -> assertThat(e).isInstanceOf(EntregaNoConcretada.class));
    }
}
