package com.nurtricenter.logisticdelivery.application;

/**
 * Se lanza cuando un caso de uso no encuentra el agregado sobre el que debe operar.
 * La capa REST la mapea a 404 (Fase 3).
 */
public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException(String message) {
        super(message);
    }
}
