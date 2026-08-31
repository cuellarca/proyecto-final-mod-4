package com.nurtricenter.logisticdelivery.application.port.out;

import com.nurtricenter.logisticdelivery.domain.shared.DomainEvent;

import java.util.List;

/**
 * Puerto de salida para publicar los eventos de dominio producidos por un agregado.
 * La implementacion (adaptador Outbox en infraestructura) los escribe en la tabla {@code outbox}
 * dentro de la <b>misma transaccion</b> del caso de uso; un publicador {@code @Scheduled} los
 * drena luego hacia RabbitMQ (Outbox transaccional manual).
 */
public interface PublicadorDeEventos {

    /** Registra los eventos pendientes de publicacion (no-op si la lista esta vacia). */
    void publicar(List<DomainEvent> eventos);
}
