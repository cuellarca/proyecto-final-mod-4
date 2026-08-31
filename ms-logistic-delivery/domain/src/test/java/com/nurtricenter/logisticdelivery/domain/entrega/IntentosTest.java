package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentosTest {

    @Test
    @DisplayName("El contador inicial tiene saldo y arranca en cero")
    void inicialTieneSaldo() {
        Intentos intentos = Intentos.inicial(2);
        assertTrue(intentos.haySaldo());
        assertEquals(0, intentos.valor());
    }

    @Test
    @DisplayName("Incrementar consume saldo y devuelve un nuevo VO")
    void incrementarConsumeSaldo() {
        Intentos intentos = Intentos.inicial(1).incrementar();
        assertEquals(1, intentos.valor());
        assertFalse(intentos.haySaldo());
    }

    @Test
    @DisplayName("Incrementar sin saldo lanza excepcion de dominio")
    void incrementarSinSaldoFalla() {
        Intentos sinSaldo = Intentos.inicial(1).incrementar();
        assertThrows(DomainException.class, sinSaldo::incrementar);
    }

    @Test
    @DisplayName("Rechaza un maximo de reintentos invalido")
    void rechazaMaximoInvalido() {
        assertThrows(DomainException.class, () -> Intentos.inicial(0));
    }
}
