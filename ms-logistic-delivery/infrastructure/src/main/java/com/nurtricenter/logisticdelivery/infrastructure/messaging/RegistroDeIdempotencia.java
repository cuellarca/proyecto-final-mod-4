package com.nurtricenter.logisticdelivery.infrastructure.messaging;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotencia basica en memoria para los consumidores: recuerda los {@code eventId} ya procesados
 * por cada consumidor y evita reprocesar duplicados (la entrega Outbox es al-menos-una-vez). Para un
 * proyecto de diplomado basta en memoria; en produccion seria una tabla de deduplicacion.
 */
@Component
public class RegistroDeIdempotencia {

    private final Map<String, Set<String>> vistosPorConsumidor = new ConcurrentHashMap<>();

    /** Devuelve {@code true} si es la primera vez que este consumidor ve el evento (y lo registra). */
    public boolean primeraVez(String consumidor, String eventId) {
        return vistosPorConsumidor
                .computeIfAbsent(consumidor, k -> ConcurrentHashMap.newKeySet())
                .add(eventId);
    }
}
