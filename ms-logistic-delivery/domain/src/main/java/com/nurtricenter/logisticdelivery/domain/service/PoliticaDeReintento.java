package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.entrega.Intentos;

import java.util.Objects;

/**
 * Domain Service: decide, ante un fallo, si corresponde reintentar o marcar la
 * entrega como no concretada (HU-5). Es puro y testeable: solo depende del
 * contador de {@code Intentos}, sin reloj ni estado propio.
 */
public class PoliticaDeReintento {

    public enum Decision {
        REINTENTAR,
        NO_CONCRETAR
    }

    public Decision decidir(Intentos intentos) {
        Objects.requireNonNull(intentos, "Intentos requerido");
        return intentos.haySaldo() ? Decision.REINTENTAR : Decision.NO_CONCRETAR;
    }
}
