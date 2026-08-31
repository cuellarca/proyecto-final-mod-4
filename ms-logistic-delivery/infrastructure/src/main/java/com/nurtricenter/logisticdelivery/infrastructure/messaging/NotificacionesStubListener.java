package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub de {@code ms-notificaciones}: consume {@code EntregaConfirmada} y {@code EntregaFallida} y
 * loguea el aviso al paciente. Deduplica por {@code eventId} (idempotencia). Expone los eventos
 * recibidos para observabilidad en la demo y los tests.
 */
@Component
public class NotificacionesStubListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesStubListener.class);
    private static final String CONSUMIDOR = "ms-notificaciones";

    private final RegistroDeIdempotencia idempotencia;
    private final List<EventoIntegracion> recibidos = new CopyOnWriteArrayList<>();

    public NotificacionesStubListener(RegistroDeIdempotencia idempotencia) {
        this.idempotencia = idempotencia;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_NOTIFICACIONES)
    public void onEvento(EventoIntegracion evento) {
        if (!idempotencia.primeraVez(CONSUMIDOR, evento.eventId())) {
            log.info("[ms-notificaciones] evento {} ya procesado; se ignora (idempotencia)", evento.eventId());
            return;
        }
        String paciente = evento.payload().path("pacienteId").asText("desconocido");
        log.info("[ms-notificaciones] Aviso al paciente {}: {} (evento {})",
                paciente, evento.type(), evento.eventId());
        recibidos.add(evento);
    }

    /** Eventos ya procesados (observabilidad para la demo/tests). */
    public List<EventoIntegracion> recibidos() {
        return List.copyOf(recibidos);
    }
}
