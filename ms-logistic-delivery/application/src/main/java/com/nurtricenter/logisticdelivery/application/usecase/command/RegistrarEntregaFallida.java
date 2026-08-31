package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso HU-5: registra el fallo de una entrega con su motivo obligatorio y deja el evento
 * {@code EntregaFallida} en el Outbox, en la misma transaccion.
 */
@Service
public class RegistrarEntregaFallida {

    private final EntregaRepository entregaRepository;
    private final PublicadorDeEventos publicador;
    private final Clock clock;

    public RegistrarEntregaFallida(EntregaRepository entregaRepository, PublicadorDeEventos publicador, Clock clock) {
        this.entregaRepository = entregaRepository;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public void ejecutar(RegistrarEntregaFallidaCommand comando) {
        Entrega entrega = entregaRepository.buscarPorId(EntregaId.de(comando.entregaId()))
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Entrega no encontrada: " + comando.entregaId()));

        entrega.registrarFallo(comando.motivo(), Instant.now(clock));

        entregaRepository.guardar(entrega);
        publicador.publicar(entrega.pullDomainEvents());
    }
}
