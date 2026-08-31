package com.nurtricenter.logisticdelivery.domain.entrega;

/** Ciclo de vida de una {@code Entrega}. */
public enum EstadoEntrega {
    PENDIENTE,
    ENTREGADA,
    FALLIDA,
    NO_CONCRETADA;

    /** Estados finales: no admiten mas transiciones. */
    public boolean esTerminal() {
        return this == ENTREGADA || this == NO_CONCRETADA;
    }
}
