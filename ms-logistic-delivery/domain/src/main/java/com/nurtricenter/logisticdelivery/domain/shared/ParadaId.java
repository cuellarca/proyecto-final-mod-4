package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;
import java.util.UUID;

/** Identidad de la entidad {@code Parada} dentro del agregado RutaDeEntrega. */
public record ParadaId(UUID valor) {

    public ParadaId {
        Objects.requireNonNull(valor, "ParadaId no puede ser nulo");
    }

    public static ParadaId nuevo() {
        return new ParadaId(UUID.randomUUID());
    }

    public static ParadaId de(String valor) {
        return new ParadaId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
