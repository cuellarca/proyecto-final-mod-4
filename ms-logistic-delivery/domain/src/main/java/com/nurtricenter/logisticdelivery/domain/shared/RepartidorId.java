package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;

/**
 * Referencia por identidad al repartidor (vive en el BC de Identidad).
 * Value Object modelado como texto por provenir de otro contexto acotado.
 */
public record RepartidorId(String valor) {

    public RepartidorId {
        Objects.requireNonNull(valor, "RepartidorId no puede ser nulo");
        if (valor.isBlank()) {
            throw new DomainException("RepartidorId no puede estar vacio");
        }
    }

    public static RepartidorId de(String valor) {
        return new RepartidorId(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
