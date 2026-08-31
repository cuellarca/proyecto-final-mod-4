package com.nurtricenter.logisticdelivery.infrastructure.acl;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorDeRutas;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorPorCercania;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * ACL (Anti-Corruption Layer) al proveedor de mapas para la optimizacion de rutas (HU-3).
 * Envuelve la llamada externa con Resilience4j — <b>timeout</b> ({@code TimeLimiter}),
 * <b>retry</b> y <b>circuit breaker</b> — y, si el proveedor no responde, <b>degrada</b> al
 * fallback puro de dominio {@link OptimizadorPorCercania} (Haversine). Es el
 * {@code OptimizadorDeRutas} primario que inyecta el caso de uso de planificacion.
 */
@Component
@Primary
public class OptimizadorDeMapasAcl implements OptimizadorDeRutas {

    private static final Logger log = LoggerFactory.getLogger(OptimizadorDeMapasAcl.class);
    private static final String INSTANCIA = "mapsProvider";

    private final ProveedorDeMapasStub proveedor;
    private final OptimizadorPorCercania fallback;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public OptimizadorDeMapasAcl(ProveedorDeMapasStub proveedor,
                                 OptimizadorPorCercania fallback,
                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                 RetryRegistry retryRegistry,
                                 TimeLimiterRegistry timeLimiterRegistry) {
        this.proveedor = proveedor;
        this.fallback = fallback;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCIA);
        this.retry = retryRegistry.retry(INSTANCIA);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(INSTANCIA);
    }

    @Override
    public List<Parada> optimizar(Geolocalizacion origen, List<Parada> paradas) {
        Supplier<CompletableFuture<List<Parada>>> futuro =
                () -> CompletableFuture.supplyAsync(() -> proveedor.optimizar(origen, paradas));

        Callable<List<Parada>> conTimeout = TimeLimiter.decorateFutureSupplier(timeLimiter, futuro);
        Callable<List<Parada>> protegido =
                CircuitBreaker.decorateCallable(circuitBreaker,
                        Retry.decorateCallable(retry, conTimeout));

        try {
            return protegido.call();
        } catch (Exception e) {
            log.warn("Proveedor de mapas no disponible ({}). Degradando a optimizacion por cercania (Haversine, HU-3).",
                    e.toString());
            return fallback.optimizar(origen, paradas);
        }
    }
}
