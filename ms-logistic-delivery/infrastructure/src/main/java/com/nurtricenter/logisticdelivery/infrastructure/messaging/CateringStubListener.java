package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub de {@code ms-catering-suscripcion}: consume {@code EntregaNoConcretada} y loguea la
 * reprogramacion de la entrega a un dia futuro. Deduplica por {@code eventId} (idempotencia).
 */
@Component
public class CateringStubListener {

    private static final Logger log = LoggerFactory.getLogger(CateringStubListener.class);
    private static final String CONSUMIDOR = "ms-catering-suscripcion";

    private final RegistroDeIdempotencia idempotencia;
    private final List<EventoIntegracion> recibidos = new CopyOnWriteArrayList<>();

    public CateringStubListener(RegistroDeIdempotencia idempotencia) {
        this.idempotencia = idempotencia;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_CATERING)
    public void onEvento(EventoIntegracion evento) {
        if (!idempotencia.primeraVez(CONSUMIDOR, evento.eventId())) {
            log.info("[ms-catering-suscripcion] evento {} ya procesado; se ignora (idempotencia)", evento.eventId());
            return;
        }
        String paciente = evento.payload().path("pacienteId").asText("desconocido");
        log.info("[ms-catering-suscripcion] Reprogramar la entrega del paciente {} a un dia futuro (evento {})",
                paciente, evento.eventId());
        recibidos.add(evento);
    }

    /** Eventos ya procesados (observabilidad para la demo/tests). */
    public List<EventoIntegracion> recibidos() {
        return List.copyOf(recibidos);
    }
}
