package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.entrega.EstadoEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.MotivoFallo;
import com.nurtricenter.logisticdelivery.domain.entrega.event.EntregaFallida;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;
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
class RegistrarEntregaFallidaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    EntregaRepository entregaRepository;
    @Mock
    PublicadorDeEventos publicador;
    @Captor
    ArgumentCaptor<List<DomainEvent>> eventosCaptor;

    @Test
    void registraElFalloConMotivoGuardaYPublicaElEvento() {
        Entrega entrega = Entrega.programar(PaqueteId.de("pkg-1"), PacienteId.de("pac-1"), RutaId.nuevo(), 2);
        when(entregaRepository.buscarPorId(entrega.id())).thenReturn(Optional.of(entrega));
        RegistrarEntregaFallida useCase = new RegistrarEntregaFallida(entregaRepository, publicador, RELOJ);

        useCase.ejecutar(new RegistrarEntregaFallidaCommand(entrega.id().toString(), MotivoFallo.AUSENTE));

        assertThat(entrega.estado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(entrega.motivoFallo()).contains(MotivoFallo.AUSENTE);
        verify(entregaRepository).guardar(entrega);
        verify(publicador).publicar(eventosCaptor.capture());
        assertThat(eventosCaptor.getValue())
                .anySatisfy(e -> assertThat(e).isInstanceOf(EntregaFallida.class));
    }
}
