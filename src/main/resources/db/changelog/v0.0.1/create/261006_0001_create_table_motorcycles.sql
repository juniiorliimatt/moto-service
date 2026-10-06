-- liquibase formatted sql

-- changeset moto-service:motorcycles-v1-create context:structure labels:motorcycles
-- comment: Motos do usuário (owner_username vem do token, nunca do payload). initial_odometer_km é o piso do hodômetro: nenhum registro da moto pode ficar abaixo dele.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'motorcycles'
CREATE TABLE moto.motorcycles
(
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname             VARCHAR(60)  NOT NULL,
    brand                VARCHAR(60),
    model                VARCHAR(60)  NOT NULL,
    model_year           INTEGER,
    plate                VARCHAR(10),
    initial_odometer_km  INTEGER      NOT NULL CHECK (initial_odometer_km >= 0),
    tank_capacity_liters NUMERIC(5, 2),
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    owner_username       VARCHAR(255) NOT NULL,
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6),
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50)
);
CREATE INDEX idx_motorcycles_owner ON moto.motorcycles (owner_username);

-- changeset moto-service:motorcycles-v1-aud context:structure labels:motorcycles,audit
-- comment: Histórico Envers de motorcycles — espelha as colunas da tabela principal (rev/revtype são os nomes default do Envers).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'motorcycles_aud'
CREATE TABLE moto.motorcycles_aud
(
    id                   UUID    NOT NULL,
    rev                  INTEGER NOT NULL REFERENCES moto.rev_info (id),
    revtype              SMALLINT,
    nickname             VARCHAR(60),
    brand                VARCHAR(60),
    model                VARCHAR(60),
    model_year           INTEGER,
    plate                VARCHAR(10),
    initial_odometer_km  INTEGER,
    tank_capacity_liters NUMERIC(5, 2),
    active               BOOLEAN,
    owner_username       VARCHAR(255),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6),
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    PRIMARY KEY (id, rev)
);
