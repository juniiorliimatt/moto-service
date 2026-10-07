-- liquibase formatted sql

-- changeset moto-service:oil-changes-v1-create context:structure labels:oil
-- comment: Trocas de óleo da moto. interval_km e interval_months são o intervalo efetivamente escolhido (o padrão por tipo de óleo só pré-preenche o formulário). A próxima troca é a que vencer primeiro, por km ou por tempo. Cascata na exclusão da moto.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'oil_changes'
CREATE TABLE moto.oil_changes
(
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    motorcycle_id   UUID           NOT NULL REFERENCES moto.motorcycles (id) ON DELETE CASCADE,
    date            DATE           NOT NULL,
    odometer_km     INTEGER        NOT NULL CHECK (odometer_km >= 0),
    oil_type        VARCHAR(30)    NOT NULL,
    brand           VARCHAR(60),
    viscosity       VARCHAR(20),
    cost            NUMERIC(10, 2) CHECK (cost IS NULL OR cost >= 0),
    interval_km     INTEGER        NOT NULL CHECK (interval_km > 0),
    interval_months INTEGER        NOT NULL CHECK (interval_months > 0),
    owner_username  VARCHAR(255)   NOT NULL,
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);
CREATE INDEX idx_oil_changes_moto_date ON moto.oil_changes (motorcycle_id, date);

-- changeset moto-service:oil-changes-v1-aud context:structure labels:oil,audit
-- comment: Histórico Envers de oil_changes — espelha as colunas da tabela principal.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'oil_changes_aud'
CREATE TABLE moto.oil_changes_aud
(
    id              UUID    NOT NULL,
    rev             INTEGER NOT NULL REFERENCES moto.rev_info (id),
    revtype         SMALLINT,
    motorcycle_id   UUID,
    date            DATE,
    odometer_km     INTEGER,
    oil_type        VARCHAR(30),
    brand           VARCHAR(60),
    viscosity       VARCHAR(20),
    cost            NUMERIC(10, 2),
    interval_km     INTEGER,
    interval_months INTEGER,
    owner_username  VARCHAR(255),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    PRIMARY KEY (id, rev)
);
