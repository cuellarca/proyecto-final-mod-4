package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;

/**
 * Contador de reintentos de una entrega (Value Object inmutable).
 * Encapsula la regla del reintento; lo consume el {@code PoliticaDeReintento}.
 *
 * @param valor  reintentos ya consumidos
 * @param maximo tope de reintentos permitidos
 */
public record Intentos(int valor, int maximo) {

    public Intentos {
        if (maximo < 1) {
            throw new DomainException("El maximo de reintentos debe ser >= 1");
        }
        if (valor < 0) {
            throw new DomainException("El contador de intentos no puede ser negativo");
        }
        if (valor > maximo) {
            throw new DomainException("Los intentos consumidos no pueden superar el maximo");
        }
    }

    /** Contador recien creado, sin reintentos consumidos. */
    public static Intentos inicial(int maximo) {
        return new Intentos(0, maximo);
    }

    /** Queda saldo mientras no se alcance el maximo. */
    public boolean haySaldo() {
        return valor < maximo;
    }

    /** Devuelve un nuevo contador con un reintento mas (falla si no hay saldo). */
    public Intentos incrementar() {
        if (!haySaldo()) {
            throw new DomainException("No quedan reintentos disponibles");
        }
        return new Intentos(valor + 1, maximo);
    }
}
