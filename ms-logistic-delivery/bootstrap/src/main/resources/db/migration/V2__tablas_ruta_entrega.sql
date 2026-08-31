-- V2: esquema de escritura de los agregados RutaDeEntrega y Entrega (Fase 1).

-- Agregado RutaDeEntrega (raiz)
CREATE TABLE ruta (
    id             UUID         PRIMARY KEY,
    repartidor_id  VARCHAR(100) NOT NULL,
    fecha          DATE         NOT NULL,
    estado         VARCHAR(20)  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0    -- optimistic locking (@Version)
);

CREATE INDEX idx_ruta_repartidor_fecha ON ruta (repartidor_id, fecha);

-- Entidad Parada (hija de la ruta; geolocalizada, HU-2)
CREATE TABLE parada (
    id           UUID                    PRIMARY KEY,
    ruta_id      UUID                    NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    paquete_id   VARCHAR(100)            NOT NULL,
    paciente_id  VARCHAR(100)            NOT NULL,
    orden        INTEGER                 NOT NULL,
    geo          geometry(Point, 4326)   NOT NULL,   -- PostGIS: coordenada de la parada
    estado       VARCHAR(20)             NOT NULL
);

CREATE INDEX idx_parada_ruta ON parada (ruta_id);
CREATE INDEX idx_parada_geo  ON parada USING GIST (geo);

-- Agregado Entrega (raiz). Se relaciona con la ruta por identidad (ruta_id), sin FK dura.
CREATE TABLE entrega (
    id                    UUID         PRIMARY KEY,
    paquete_id            VARCHAR(100) NOT NULL,
    paciente_id           VARCHAR(100) NOT NULL,
    ruta_id               UUID         NOT NULL,
    estado                VARCHAR(20)  NOT NULL,
    intentos_valor        INTEGER      NOT NULL,
    intentos_maximo       INTEGER      NOT NULL,
    -- fallo (HU-5): presente solo cuando estado = FALLIDA
    motivo_fallo          VARCHAR(30),
    -- constancia (HU-4): presente solo cuando estado = ENTREGADA
    constancia_timestamp  TIMESTAMPTZ,
    constancia_lat        DOUBLE PRECISION,
    constancia_lon        DOUBLE PRECISION,
    constancia_url        VARCHAR(500),
    constancia_receptor   VARCHAR(200),
    version               BIGINT       NOT NULL DEFAULT 0    -- optimistic locking (@Version)
);

CREATE INDEX idx_entrega_paciente ON entrega (paciente_id);
CREATE INDEX idx_entrega_ruta     ON entrega (ruta_id);
