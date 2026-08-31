package com.nurtricenter.logisticdelivery.application.usecase.command;

import com.nurtricenter.logisticdelivery.application.EntidadNoEncontradaException;
import com.nurtricenter.logisticdelivery.application.port.out.PublicadorDeEventos;
import com.nurtricenter.logisticdelivery.domain.entrega.ConstanciaDeEntrega;
import com.nurtricenter.logisticdelivery.domain.entrega.Entrega;
import com.nurtricenter.logisticdelivery.domain.repository.EntregaRepository;
import com.nurtricenter.logisticdelivery.domain.shared.EntregaId;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso HU-4: confirma una entrega con su constancia completa y deja el evento
 * {@code EntregaConfirmada} en el Outbox, en la misma transaccion.
 */
@Service
public class ConfirmarEntrega {

    private final EntregaRepository entregaRepository;
    private final PublicadorDeEventos publicador;
    private final Clock clock;

    public ConfirmarEntrega(EntregaRepository entregaRepository, PublicadorDeEventos publicador, Clock clock) {
        this.entregaRepository = entregaRepository;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public void ejecutar(ConfirmarEntregaCommand comando) {
        Entrega entrega = entregaRepository.buscarPorId(EntregaId.de(comando.entregaId()))
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Entrega no encontrada: " + comando.entregaId()));

        ConstanciaDeEntrega constancia = new ConstanciaDeEntrega(
                Instant.now(clock),
                new Geolocalizacion(comando.lat(), comando.lon()),
                Url.de(comando.urlEvidencia()),
                comando.nombreReceptor());

        entrega.confirmar(constancia);

        entregaRepository.guardar(entrega);
        publicador.publicar(entrega.pullDomainEvents());
    }
}
