package com.nurtricenter.logisticdelivery.infrastructure.messaging;

/**
 * Mapeo del tipo de evento de dominio a su routing key de salida en el exchange topico. Cada tipo
 * viaja con su propia key para que cada consumidor se suscriba solo a lo que le interesa.
 */
public final class RoutingKeys {

    public static final String RUTA_PLANIFICADA = "logistica.ruta-de-entrega-planificada";
    public static final String ENTREGA_CONFIRMADA = "logistica.entrega-confirmada";
    public static final String ENTREGA_FALLIDA = "logistica.entrega-fallida";
    public static final String ENTREGA_REPROGRAMADA = "logistica.entrega-reprogramada";
    public static final String ENTREGA_NO_CONCRETADA = "logistica.entrega-no-concretada";

    private RoutingKeys() {
    }

    /** Routing key para un tipo de evento (nombre simple de la clase del evento de dominio). */
    public static String paraTipo(String eventType) {
        return switch (eventType) {
            case "RutaDeEntregaPlanificada" -> RUTA_PLANIFICADA;
            case "EntregaConfirmada" -> ENTREGA_CONFIRMADA;
            case "EntregaFallida" -> ENTREGA_FALLIDA;
            case "EntregaReprogramada" -> ENTREGA_REPROGRAMADA;
            case "EntregaNoConcretada" -> ENTREGA_NO_CONCRETADA;
            default -> "logistica.desconocido";
        };
    }
}
