-- liquibase formatted sql

-- changeset moto-service:envers-v1-rev-info context:structure labels:audit
-- comment: Tabela de revisões do Hibernate Envers, compartilhada entre todas as entidades @Audited. Colunas id/timestamp são o mapeamento real de DefaultRevisionEntity (int id, long timestamp). @GeneratedValue sem estratégia explícita resolve pra SEQUENCE no Postgres (Hibernate 6), convenção de nome "{table}_seq", com INCREMENT BY 50 (allocationSize default do JPA/Hibernate — tem que bater exatamente). username vem do RevisionListenerImpl (SecurityContext). Mesmo padrão do budget-service.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'moto' AND table_name = 'rev_info'
CREATE SEQUENCE moto.rev_info_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE moto.rev_info
(
    id        INTEGER PRIMARY KEY DEFAULT nextval('moto.rev_info_seq'),
    timestamp BIGINT,
    username  VARCHAR(255) NOT NULL
);
