package com.nurtricenter.logisticdelivery.domain.shared;

/**
 * Se lanza cuando una operacion intenta violar una invariante del dominio.
 * Es el mecanismo con el que las raices de agregado protegen su consistencia.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
