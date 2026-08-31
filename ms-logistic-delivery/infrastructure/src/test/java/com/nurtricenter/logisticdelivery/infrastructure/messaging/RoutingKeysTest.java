package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapeo de tipo de evento a routing key. Es contrato publicado: si una key cambia, los consumidores
 * de los otros bounded contexts dejan de recibir sus eventos sin que nada falle a la vista. Prueba
 * parametrizada porque es la misma regla con varios casos.
 */
class RoutingKeysTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "RutaDeEntregaPlanificada, logistica.ruta-de-entrega-planificada",
            "EntregaConfirmada,        logistica.entrega-confirmada",
            "EntregaFallida,           logistica.entrega-fallida",
            "EntregaReprogramada,      logistica.entrega-reprogramada",
            "EntregaNoConcretada,      logistica.entrega-no-concretada"
    })
    @DisplayName("Cada evento publicado viaja por su propia routing key")
    void cadaEventoTieneSuRoutingKey(String tipoDeEvento, String routingKeyEsperada) {
        assertThat(RoutingKeys.paraTipo(tipoDeEvento)).isEqualTo(routingKeyEsperada);
    }

    @ParameterizedTest
    @CsvSource({"EventoDelFuturo", "''"})
    @DisplayName("Un tipo desconocido no rompe la publicacion: cae en la key de descarte")
    void unTipoDesconocidoCaeEnLaKeyDeDescarte(String tipoDesconocido) {
        assertThat(RoutingKeys.paraTipo(tipoDesconocido)).isEqualTo("logistica.desconocido");
    }
}
