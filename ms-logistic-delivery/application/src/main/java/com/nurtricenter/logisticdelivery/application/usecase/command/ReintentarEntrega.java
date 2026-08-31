package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.service.PoliticaDeReintento;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso HU-5: aplica la politica de reintento sobre una entrega fallida. Segun el saldo de
 * intentos, reprograma (evento {@code EntregaReprogramada}) o la marca no concretada
 * ({@code EntregaNoConcretada}). Deja el evento en el Outbox, en la misma transaccion.
 */
@Service
public class ReintentarEntrega {

    private final EntregaRepository entregaRepository;
    private final PoliticaDeReintento politica;
    private final PublicadorDeEventos publicador;
    private final Clock clock;

    public ReintentarEntrega(EntregaRepository entregaRepository, PoliticaDeReintento politica,
                             PublicadorDeEventos publicador, Clock clock) {
        this.entregaRepository = entregaRepository;
        this.politica = politica;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public void ejecutar(ReintentarEntregaCommand comando) {
        Entrega entrega = entregaRepository.buscarPorId(EntregaId.de(comando.entregaId()))
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Entrega no encontrada: " + comando.entregaId()));

        entrega.reintentar(politica, Instant.now(clock));

        entregaRepository.guardar(entrega);
        publicador.publicar(entrega.pullDomainEvents());
    }
}
