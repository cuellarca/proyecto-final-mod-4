package com.nurtricenter.logisticdelivery.infrastructure.acl;

import com.nurtricenter.logisticdelivery.domain.ruta.Parada;
import com.nurtricenter.logisticdelivery.domain.service.OptimizadorPorCercania;
import com.nurtricenter.logisticdelivery.domain.shared.Destino;
import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub del proveedor externo de mapas. Simula el servicio real con <b>latencia</b> y una
 * <b>tasa de fallos</b> configurables ({@code logistic.maps.*}) para poder demostrar la
 * degradacion resiliente (HU-3). No lleva Resilience4j: eso lo aportan los ACL que lo envuelven
 * ({@link OptimizadorDeMapasAcl}, {@link GeocodificadorAcl}).
 */
@Component
public class ProveedorDeMapasStub {

    private static final Logger log = LoggerFactory.getLogger(ProveedorDeMapasStub.class);

    private final long latenciaMs;
    private final double tasaFallo;
    // Punto base del geocodificador (por defecto: centro aprox. de Santa Cruz de la Sierra).
    private final double latBase;
    private final double lonBase;
    // El proveedor "bueno" tambien ordena por cercania; la gracia es que puede caerse.
    private final OptimizadorPorCercania ordenador = new OptimizadorPorCercania();

    public ProveedorDeMapasStub(
            @Value("${logistic.maps.stub-latency-ms:200}") long latenciaMs,
            @Value("${logistic.maps.stub-failure-rate:0.0}") double tasaFallo,
            @Value("${logistic.maps.base-lat:-17.7833}") double latBase,
            @Value("${logistic.maps.base-lon:-63.1821}") double lonBase) {
        this.latenciaMs = latenciaMs;
        this.tasaFallo = tasaFallo;
        this.latBase = latBase;
        this.lonBase = lonBase;
    }

    /** Secuencia optima de paradas segun el "proveedor" (puede fallar/tardar). */
    public List<Parada> optimizar(Geolocalizacion origen, List<Parada> paradas) {
        simularLlamadaExterna("optimizar");
        return ordenador.optimizar(origen, paradas);
    }

    /** Coordenadas de una direccion segun el "proveedor" (puede fallar/tardar). */
    public Geolocalizacion geocodificar(Destino destino) {
        simularLlamadaExterna("geocodificar");
        int hash = destino.texto().hashCode();
        double dLat = ((hash % 200) / 10_000.0);
        double dLon = (((hash / 200) % 200) / 10_000.0);
        return new Geolocalizacion(latBase + dLat, lonBase + dLon);
    }

    private void simularLlamadaExterna(String operacion) {
        if (latenciaMs > 0) {
            try {
                Thread.sleep(latenciaMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProveedorDeMapasNoDisponibleException("Llamada al proveedor interrumpida");
            }
        }
        if (tasaFallo > 0 && ThreadLocalRandom.current().nextDouble() < tasaFallo) {
            log.warn("[maps-stub] fallo simulado en '{}' (tasaFallo={})", operacion, tasaFallo);
            throw new ProveedorDeMapasNoDisponibleException("Proveedor de mapas caido (fallo simulado)");
        }
    }
}
