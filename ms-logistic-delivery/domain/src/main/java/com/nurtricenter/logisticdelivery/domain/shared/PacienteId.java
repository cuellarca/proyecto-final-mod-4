package com.nurtricenter.logisticdelivery.domain.shared;

import java.util.Objects;

/**
 * Referencia por identidad al paciente (vive en el BC de Pacientes/Suscripcion).
 * Value Object modelado como texto por provenir de otro contexto acotado.
 */
public record PacienteId(String valor) {

    public PacienteId {
        Objects.requireNonNull(valor, "PacienteId no puede ser nulo");
        if (valor.isBlank()) {
            throw new DomainException("PacienteId no puede estar vacio");
        }
    }

    public static PacienteId de(String valor) {
        return new PacienteId(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
