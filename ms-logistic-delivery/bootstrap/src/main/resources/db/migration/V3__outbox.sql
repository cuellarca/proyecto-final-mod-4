-- V3: tabla Outbox transaccional. Se escribe en la MISMA transaccion que el agregado (Fase 2);
-- un publicador @Scheduled la drena hacia RabbitMQ y marca las filas como publicadas (Fase 4).
CREATE TABLE outbox (
    id            UUID          PRIMARY KEY,
    event_type    VARCHAR(100)  NOT NULL,       -- tipo del evento de dominio
    aggregate_id  VARCHAR(100),                 -- id del agregado que lo emitio (para trazabilidad/proyeccion)
    payload       JSONB         NOT NULL,        -- evento serializado (published language, Jackson)
    occurred_at   TIMESTAMPTZ   NOT NULL,        -- cuando ocurrio el hecho de dominio
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published     BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMPTZ
);

-- Indice parcial para drenar eficientemente solo lo pendiente, en orden de creacion.
CREATE INDEX idx_outbox_pendientes ON outbox (created_at) WHERE published = FALSE;
