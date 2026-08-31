-- V4: read model desnormalizado del historial de entregas por paciente (CQRS lectura, HU-6).
-- Lo puebla el ProyectorHistorial consumiendo los eventos; las consultas leen solo de aqui.
CREATE TABLE historial_entrega_paciente (
    entrega_id            UUID          PRIMARY KEY,
    paciente_id           VARCHAR(100)  NOT NULL,
    paquete_id            VARCHAR(100),
    -- estado del read model segun el ultimo evento: CONFIRMADA / FALLIDA / REPROGRAMADA / NO_CONCRETADA
    estado                VARCHAR(20)   NOT NULL,
    motivo_fallo          VARCHAR(30),
    intentos              INTEGER,
    -- constancia (HU-4/HU-6): evidencia servida desde el read model
    constancia_timestamp  TIMESTAMPTZ,
    constancia_lat        DOUBLE PRECISION,
    constancia_lon        DOUBLE PRECISION,
    constancia_url        VARCHAR(500),
    constancia_receptor   VARCHAR(200),
    ultimo_evento_en      TIMESTAMPTZ   NOT NULL,
    actualizado_en        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_historial_paciente        ON historial_entrega_paciente (paciente_id);
CREATE INDEX idx_historial_paciente_fecha  ON historial_entrega_paciente (paciente_id, ultimo_evento_en);
