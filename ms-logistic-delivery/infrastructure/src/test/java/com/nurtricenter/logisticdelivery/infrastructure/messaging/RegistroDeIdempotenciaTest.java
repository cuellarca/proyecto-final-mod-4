package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroDeIdempotenciaTest {

    private final RegistroDeIdempotencia registro = new RegistroDeIdempotencia();

    @Test
    void reconoceLaPrimeraVezYLuegoDeduplica() {
        assertThat(registro.primeraVez("ms-notificaciones", "evt-1")).isTrue();
        assertThat(registro.primeraVez("ms-notificaciones", "evt-1")).isFalse();
        assertThat(registro.primeraVez("ms-notificaciones", "evt-2")).isTrue();
    }

    @Test
    void cadaConsumidorDeduplicaDeFormaIndependiente() {
        assertThat(registro.primeraVez("ms-notificaciones", "evt-1")).isTrue();
        // Otro consumidor ve el mismo evento por primera vez.
        assertThat(registro.primeraVez("ms-catering-suscripcion", "evt-1")).isTrue();
    }
}
