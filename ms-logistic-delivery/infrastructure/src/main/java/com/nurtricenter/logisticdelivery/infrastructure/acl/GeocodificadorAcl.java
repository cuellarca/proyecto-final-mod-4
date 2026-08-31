package com.nurtricenter.logisticdelivery.infrastructure.acl;

import com.nurtricenter.logisticdelivery.domain.service.Geocodificador;
import com.nurtricenter.logisticdelivery.domain.shared.Destino;
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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * ACL al proveedor de mapas para geocodificar (HU-2). Envuelve la llamada externa con Resilience4j
 * (timeout + retry + circuit breaker). A diferencia de la optimizacion, geocodificar no tiene un
 * fallback puro: si el proveedor no responde, propaga {@link ProveedorDeMapasNoDisponibleException}
 * (la capa REST la mapea a 503).
 */
@Component
@Primary
public class GeocodificadorAcl implements Geocodificador {

    private static final Logger log = LoggerFactory.getLogger(GeocodificadorAcl.class);
    private static final String INSTANCIA = "mapsProvider";

    private final ProveedorDeMapasStub proveedor;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public GeocodificadorAcl(ProveedorDeMapasStub proveedor,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             RetryRegistry retryRegistry,
                             TimeLimiterRegistry timeLimiterRegistry) {
        this.proveedor = proveedor;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCIA);
        this.retry = retryRegistry.retry(INSTANCIA);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(INSTANCIA);
    }

    @Override
    public Geolocalizacion geocodificar(Destino destino) {
        Supplier<CompletableFuture<Geolocalizacion>> futuro =
                () -> CompletableFuture.supplyAsync(() -> proveedor.geocodificar(destino));

        Callable<Geolocalizacion> conTimeout = TimeLimiter.decorateFutureSupplier(timeLimiter, futuro);
        Callable<Geolocalizacion> protegido =
                CircuitBreaker.decorateCallable(circuitBreaker,
                        Retry.decorateCallable(retry, conTimeout));

        try {
            return protegido.call();
        } catch (ProveedorDeMapasNoDisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Proveedor de mapas no disponible al geocodificar '{}': {}", destino.texto(), e.toString());
            throw new ProveedorDeMapasNoDisponibleException("No se pudo geocodificar: " + destino.texto());
        }
    }
}
