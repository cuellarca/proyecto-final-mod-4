package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;

/**
 * Ubicacion de la evidencia (foto/firma) en el object storage (Value Object).
 * El dominio guarda la URL, nunca el binario.
 */
public record Url(String valor) {

    public Url {
        Objects.requireNonNull(valor, "Url no puede ser nula");
        if (valor.isBlank()) {
            throw new DomainException("Url no puede estar vacia");
        }
        if (!(valor.startsWith("http://") || valor.startsWith("https://"))) {
            throw new DomainException("Url invalida: debe iniciar con http:// o https://");
        }
    }

    public static Url de(String valor) {
        return new Url(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
