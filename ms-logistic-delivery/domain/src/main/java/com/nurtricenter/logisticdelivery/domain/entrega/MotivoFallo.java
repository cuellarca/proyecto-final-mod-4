package com.nurtricenter.logisticdelivery.domain.entrega;

/** Motivo por el que una entrega no pudo concretarse (Value Object enumerado, HU-5). */
public enum MotivoFallo {
    AUSENTE,
    DIRECCION_ERRONEA,
    RECHAZADA,
    OTRO
}
