package com.nurtricenter.logisticdelivery.infrastructure.config;

import com.nurtricenter.logisticdelivery.domain.service.OptimizadorPorCercania;
import com.nurtricenter.logisticdelivery.domain.service.PoliticaDeReintento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans transversales que la capa de aplicacion inyecta: el reloj (para instantes deterministas
 * en tests) y los servicios de dominio puros que no llevan estado.
 */
@Configuration
public class AplicacionConfig {

    /** Reloj del sistema (UTC). Los tests inyectan un {@code Clock.fixed(...)} para instantes deterministas. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Servicio de dominio de la politica de reintento (HU-5), puro y sin estado. */
    @Bean
    public PoliticaDeReintento politicaDeReintento() {
        return new PoliticaDeReintento();
    }

    /**
     * Fallback puro de dominio para la optimizacion de rutas (heuristica del vecino mas cercano,
     * HU-3). En la Fase 3 el ACL de mapas lo usa como degradacion cuando el proveedor no responde.
     * Al ser el unico {@code OptimizadorDeRutas} disponible en la Fase 2, tambien lo inyecta el caso
     * de uso de planificacion.
     */
    @Bean
    public OptimizadorPorCercania optimizadorPorCercania() {
        return new OptimizadorPorCercania();
    }
}
