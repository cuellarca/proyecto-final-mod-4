package com.nurtricenter.logisticdelivery.domain.entrega;

import com.nurtricenter.logisticdelivery.domain.shared.DomainException;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.Url;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstanciaDeEntregaTest {

    /** Instante fijo: el tiempo entra como literal, no como reloj del sistema (P4). */
    private static final Instant MOMENTO = Instant.parse("2026-07-10T12:00:00Z");

    private static final Geolocalizacion GEO = new Geolocalizacion(-17.78, -63.18);
    private static final Url EVIDENCIA = Url.de("https://storage/evidencia.jpg");

    @Test
    @DisplayName("Rechaza una constancia con receptor vacio (HU-4)")
    void rechazaReceptorVacio() {
        assertThrows(DomainException.class,
                () -> new ConstanciaDeEntrega(MOMENTO, GEO, EVIDENCIA, "  "));
    }

    @Test
    @DisplayName("Rechaza una constancia sin evidencia (HU-4)")
    void rechazaEvidenciaNula() {
        assertThrows(NullPointerException.class,
                () -> new ConstanciaDeEntrega(MOMENTO, GEO, null, "Juan"));
    }

    @Test
    @DisplayName("Una constancia valida esta completa")
    void constanciaValidaEstaCompleta() {
        ConstanciaDeEntrega constancia = new ConstanciaDeEntrega(MOMENTO, GEO, EVIDENCIA, "Juan Perez");
        assertTrue(constancia.estaCompleta());
    }
}
