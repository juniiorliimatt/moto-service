-- liquibase formatted sql

-- changeset moto-service:refuelings-v1-create context:structure labels:refuelings
-- comment: Abastecimentos da moto. odometer_km é o hodômetro TOTAL (não o trip). full_tank = tanque completado (opcional: só refina a precisão do km/l entre dois cheios). Cascata na exclusão da moto.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'refuelings'
CREATE TABLE moto.refuelings
(
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    motorcycle_id  UUID          NOT NULL REFERENCES moto.motorcycles (id) ON DELETE CASCADE,
    date           DATE          NOT NULL,
    odometer_km    INTEGER       NOT NULL CHECK (odometer_km >= 0),
    liters         NUMERIC(6, 2) NOT NULL CHECK (liters > 0),
    total_value    NUMERIC(10, 2) NOT NULL CHECK (total_value > 0),
    station        VARCHAR(120),
    fuel_type      VARCHAR(30)   NOT NULL,
    full_tank      BOOLEAN       NOT NULL DEFAULT FALSE,
    owner_username VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMP(6),
    updated_at     TIMESTAMP(6),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);
CREATE INDEX idx_refuelings_moto_date ON moto.refuelings (motorcycle_id, date, odometer_km);

-- changeset moto-service:refuelings-v1-aud context:structure labels:refuelings,audit
-- comment: Histórico Envers de refuelings — espelha as colunas da tabela principal.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'refuelings_aud'
CREATE TABLE moto.refuelings_aud
(
    id             UUID    NOT NULL,
    rev            INTEGER NOT NULL REFERENCES moto.rev_info (id),
    revtype        SMALLINT,
    motorcycle_id  UUID,
    date           DATE,
    odometer_km    INTEGER,
    liters         NUMERIC(6, 2),
    total_value    NUMERIC(10, 2),
    station        VARCHAR(120),
    fuel_type      VARCHAR(30),
    full_tank      BOOLEAN,
    owner_username VARCHAR(255),
    created_at     TIMESTAMP(6),
    updated_at     TIMESTAMP(6),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    PRIMARY KEY (id, rev)
);
