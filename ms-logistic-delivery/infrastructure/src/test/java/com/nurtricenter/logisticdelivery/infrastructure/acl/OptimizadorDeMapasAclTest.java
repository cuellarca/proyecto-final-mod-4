package com.nurtricenter.logisticdelivery.infrastructure.acl;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorPorCercania;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import com.nurtricenter.logisticdelivery.domain.shared.PacienteId;
import com.nurtricenter.logisticdelivery.domain.shared.PaqueteId;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-3: verifica la <b>degradacion resiliente</b> del ACL de mapas. Cuando el proveedor externo no
 * responde (tasa de fallo 100%), la optimizacion cae al fallback puro de dominio
 * {@code OptimizadorPorCercania} (Haversine) y la ruta se planifica igual.
 */
class OptimizadorDeMapasAclTest {

    private final CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.of(
            RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(10)).build());
    private final TimeLimiterRegistry tlRegistry = TimeLimiterRegistry.ofDefaults();

    private List<Parada> tresParadas() {
        return List.of(
                Parada.crear(PaqueteId.de("p1"), PacienteId.de("pac1"), 1, new Geolocalizacion(-17.79, -63.19)),
                Parada.crear(PaqueteId.de("p2"), PacienteId.de("pac2"), 2, new Geolocalizacion(-17.80, -63.20)),
                Parada.crear(PaqueteId.de("p3"), PacienteId.de("pac3"), 3, new Geolocalizacion(-17.78, -63.18)));
    }

    @Test
    void degradaAHaversineCuandoElProveedorSiempreFalla() {
        ProveedorDeMapasStub proveedorCaido = new ProveedorDeMapasStub(0L, 1.0, -17.7833, -63.1821); // 100% de fallo
        OptimizadorDeMapasAcl acl = new OptimizadorDeMapasAcl(
                proveedorCaido, new OptimizadorPorCercania(), cbRegistry, retryRegistry, tlRegistry);
        List<Parada> paradas = tresParadas();

        List<Parada> secuencia = acl.optimizar(new Geolocalizacion(-17.7833, -63.1821), paradas);

        // Aun caido el proveedor, se obtiene una secuencia valida (mismas paradas) por cercania.
        assertThat(secuencia).hasSize(3).containsExactlyInAnyOrderElementsOf(paradas);
    }

    @Test
    void usaElProveedorCuandoRespondeCorrectamente() {
        ProveedorDeMapasStub proveedorSano = new ProveedorDeMapasStub(0L, 0.0, -17.7833, -63.1821); // sin fallos
        OptimizadorDeMapasAcl acl = new OptimizadorDeMapasAcl(
                proveedorSano, new OptimizadorPorCercania(), cbRegistry, retryRegistry, tlRegistry);
        List<Parada> paradas = tresParadas();

        List<Parada> secuencia = acl.optimizar(new Geolocalizacion(-17.7833, -63.1821), paradas);

        assertThat(secuencia).hasSize(3).containsExactlyInAnyOrderElementsOf(paradas);
    }
}
