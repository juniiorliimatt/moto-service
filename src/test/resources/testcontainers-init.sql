-- Espelha initdb/*.sql da raiz do monorepo (só a parte de moto_service — este
-- container não roda workbox-api). Roda como o superusuário do container
-- (POSTGRES_USER/POSTGRES_PASSWORD do PostgreSQLContainer), igual em produção.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE ROLE moto_service WITH LOGIN PASSWORD 'moto_service';
GRANT CONNECT ON DATABASE workbox TO moto_service;
CREATE SCHEMA IF NOT EXISTS moto AUTHORIZATION moto_service;
