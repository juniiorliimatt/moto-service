-- liquibase formatted sql

-- changeset moto-service:odometer-readings-v1-create context:structure labels:odometer
-- comment: Leituras avulsas de hodômetro (km informado sem abastecer). Junto com abastecimentos e trocas de óleo, formam a linha do tempo de hodômetro da moto, que é monotônica (regra validada na aplicação — ver OdometerRules). Cascata na exclusão da moto.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'odometer_readings'
CREATE TABLE moto.odometer_readings
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    motorcycle_id  UUID         NOT NULL REFERENCES moto.motorcycles (id) ON DELETE CASCADE,
    date           DATE         NOT NULL,
    odometer_km    INTEGER      NOT NULL CHECK (odometer_km >= 0),
    owner_username VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP(6),
    updated_at     TIMESTAMP(6),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);
CREATE INDEX idx_odometer_readings_moto_date ON moto.odometer_readings (motorcycle_id, date);

-- changeset moto-service:odometer-readings-v1-aud context:structure labels:odometer,audit
-- comment: Histórico Envers de odometer_readings — espelha as colunas da tabela principal.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'odometer_readings_aud'
CREATE TABLE moto.odometer_readings_aud
(
    id             UUID    NOT NULL,
    rev            INTEGER NOT NULL REFERENCES moto.rev_info (id),
    revtype        SMALLINT,
    motorcycle_id  UUID,
    date           DATE,
    odometer_km    INTEGER,
    owner_username VARCHAR(255),
    created_at     TIMESTAMP(6),
    updated_at     TIMESTAMP(6),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    PRIMARY KEY (id, rev)
);
