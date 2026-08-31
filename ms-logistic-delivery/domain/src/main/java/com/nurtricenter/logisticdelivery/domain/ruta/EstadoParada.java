package com.nurtricenter.logisticdelivery.domain.ruta;

/** Ciclo de vida de una {@code Parada} dentro de la ruta. */
public enum EstadoParada {
    PENDIENTE,
    EN_CURSO,
    COMPLETADA,
    FALLIDA;

    /** Una parada terminal ya no cambia de estado (completada o fallida). */
    public boolean esTerminal() {
        return this == COMPLETADA || this == FALLIDA;
    }
}
