package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;

/**
 * Referencia por identidad al paquete armado en el BC de Produccion (Value Object).
 * Se modela como texto porque proviene de otro contexto acotado.
 */
public record PaqueteId(String valor) {

    public PaqueteId {
        Objects.requireNonNull(valor, "PaqueteId no puede ser nulo");
        if (valor.isBlank()) {
            throw new DomainException("PaqueteId no puede estar vacio");
        }
    }

    public static PaqueteId de(String valor) {
        return new PaqueteId(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
