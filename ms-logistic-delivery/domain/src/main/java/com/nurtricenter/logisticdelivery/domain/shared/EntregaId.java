package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;
import java.util.UUID;

/** Identidad del agregado {@code Entrega} (Value Object). */
public record EntregaId(UUID valor) {

    public EntregaId {
        Objects.requireNonNull(valor, "EntregaId no puede ser nulo");
    }

    public static EntregaId nuevo() {
        return new EntregaId(UUID.randomUUID());
    }

    public static EntregaId de(String valor) {
        return new EntregaId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
