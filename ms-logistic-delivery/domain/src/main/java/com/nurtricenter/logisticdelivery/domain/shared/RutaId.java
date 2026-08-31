package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;
import java.util.UUID;

/** Identidad del agregado {@code RutaDeEntrega} (Value Object). */
public record RutaId(UUID valor) {

    public RutaId {
        Objects.requireNonNull(valor, "RutaId no puede ser nulo");
    }

    public static RutaId nuevo() {
        return new RutaId(UUID.randomUUID());
    }

    public static RutaId de(String valor) {
        return new RutaId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
