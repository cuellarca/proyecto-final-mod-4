package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Raiz de agregado: unica puerta de entrada al agregado y guardiana de sus
 * invariantes. Acumula los eventos de dominio que produce su comportamiento
 * para que la capa de aplicacion los publique tras persistir.
 *
 * @param <ID> tipo del identificador del agregado
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** Registra un evento producido por el comportamiento del agregado. */
    protected void registrarEvento(DomainEvent evento) {
        domainEvents.add(evento);
    }

    /** Devuelve una copia de los eventos acumulados y limpia el buffer. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copia = List.copyOf(domainEvents);
        domainEvents.clear();
        return copia;
    }

    /** Identidad del agregado. */
    public abstract ID id();
}
