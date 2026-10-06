package br.com.moto;

import org.junit.jupiter.api.Test;

class RealPostgresSchemaIT extends PostgresIT {

    @Test
    void contextoSobeContraPostgresReal() {
        // O teste é o próprio carregamento do contexto: se Liquibase e o mapeamento JPA
        // (inclusive as tabelas _aud do Envers) divergirem, ddl-auto=validate falha o boot.
    }
}
