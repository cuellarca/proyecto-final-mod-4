package com.nurtricenter.logisticdelivery.domain.service;

import com.nurtricenter.logisticdelivery.domain.entrega.Intentos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliticaDeReintentoTest {

    private final PoliticaDeReintento politica = new PoliticaDeReintento();

    @Test
    @DisplayName("Decide REINTENTAR mientras haya saldo")
    void reintentaSiHaySaldo() {
        assertEquals(PoliticaDeReintento.Decision.REINTENTAR, politica.decidir(Intentos.inicial(2)));
    }

    @Test
    @DisplayName("Decide NO_CONCRETAR cuando se agota el saldo")
    void noConcretaSiNoHaySaldo() {
        Intentos sinSaldo = Intentos.inicial(1).incrementar();
        assertEquals(PoliticaDeReintento.Decision.NO_CONCRETAR, politica.decidir(sinSaldo));
    }
}
