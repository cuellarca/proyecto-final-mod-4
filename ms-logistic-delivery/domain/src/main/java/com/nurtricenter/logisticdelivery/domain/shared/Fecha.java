package com.nurtricenter.logisticdelivery.domain.shared;

import java.time.LocalDate;
import java.util.Objects;

/** Dia de la entrega (Value Object). Envuelve una fecha del calendario. */
public record Fecha(LocalDate valor) {

    public Fecha {
        Objects.requireNonNull(valor, "Fecha no puede ser nula");
    }

    public static Fecha de(LocalDate valor) {
        return new Fecha(valor);
    }

    public static Fecha de(int anio, int mes, int dia) {
        return new Fecha(LocalDate.of(anio, mes, dia));
    }

    public boolean esAnteriorA(Fecha otra) {
        return this.valor.isBefore(otra.valor);
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
