package com.nurtricenter.logisticdelivery.infrastructure.acl;

import com.nurtricenter.logisticdelivery.domain.shared.Destino;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * HU-2/HU-3: el ACL de geocodificacion no tiene fallback puro (a diferencia de la optimizacion de
 * rutas): si el proveedor externo no responde, el contrato es propagar
 * {@code ProveedorDeMapasNoDisponibleException}, que el borde REST traduce a 503. Inventar
 * coordenadas seria peor que fallar.
 */
class GeocodificadorAclTest {

    private static final Destino DIRECCION = Destino.sinGeocodificar("Av. Banzer 3er anillo");

    private final CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.of(
            RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(10)).build());
    private final TimeLimiterRegistry tlRegistry = TimeLimiterRegistry.ofDefaults();

    private GeocodificadorAcl aclCon(double tasaFallo) {
        // Proveedor sin latencia, con la tasa de fallo pedida y base en Santa Cruz de la Sierra.
        return new GeocodificadorAcl(new ProveedorDeMapasStub(0L, tasaFallo, -17.7833, -63.1821),
                cbRegistry, retryRegistry, tlRegistry);
    }

    @Test
    @DisplayName("HU-2: con el proveedor sano devuelve coordenadas dentro de su zona base")
    void devuelveCoordenadasCuandoElProveedorResponde() {
        Geolocalizacion geo = aclCon(0.0).geocodificar(DIRECCION);

        // El stub desplaza como mucho 0.02 grados respecto de su punto base.
        assertThat(geo.lat()).isCloseTo(-17.7833, within(0.02));
    }

    @Test
    @DisplayName("HU-3: con el proveedor caido falla explicito (503), no inventa coordenadas")
    void propagaElFalloCuandoElProveedorEstaCaido() {
        GeocodificadorAcl acl = aclCon(1.0);

        assertThatThrownBy(() -> acl.geocodificar(DIRECCION))
                .isInstanceOf(ProveedorDeMapasNoDisponibleException.class);
    }
}
