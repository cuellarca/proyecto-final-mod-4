package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaConfirmada;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmarEntregaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    EntregaRepository entregaRepository;
    @Mock
    PublicadorDeEventos publicador;
    @Captor
    ArgumentCaptor<List<DomainEvent>> eventosCaptor;

    private Entrega entregaPendiente() {
        return Entrega.programar(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), RutaId.nuevo(), 2);
    }

    @Test
    void confirmaConConstanciaGuardaYPublicaElEvento() {
        Entrega entrega = entregaPendiente();
        when(entregaRepository.buscarPorId(entrega.id())).thenReturn(Optional.of(entrega));
        ConfirmarEntrega useCase = new ConfirmarEntrega(entregaRepository, publicador, RELOJ);

        useCase.ejecutar(new ConfirmarEntregaCommand(
                entrega.id().toString(), -17.78, -63.18,
                "https://storage.example.com/e.jpg", "Ana Lopez"));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(entrega.constancia()).isPresent();
        verify(entregaRepository).guardar(entrega);
        verify(publicador).publicar(eventosCaptor.capture());
        assertThat(eventosCaptor.getValue())
                .anySatisfy(e -> assertThat(e).isInstanceOf(EntregaConfirmada.class));
    }

    @Test
    void fallaCuandoLaEntregaNoExiste() {
        EntregaId id = EntregaId.nuevo();
        when(entregaRepository.buscarPorId(id)).thenReturn(Optional.empty());
        ConfirmarEntrega useCase = new ConfirmarEntrega(entregaRepository, publicador, RELOJ);

        assertThatThrownBy(() -> useCase.ejecutar(new ConfirmarEntregaCommand(
                id.toString(), -17.78, -63.18, "https://s/e.jpg", "Ana")))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }
}
