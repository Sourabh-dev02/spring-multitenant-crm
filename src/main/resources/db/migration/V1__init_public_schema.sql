-- V1__init_public_schema.sql
-- Flyway manages the public schema only.
-- Tenant schemas are provisioned at runtime by TenantService.provisionSchema().

-- Sequence for the tenants table PK
CREATE SEQUENCE IF NOT EXISTS public.tenant_seq
    START 1
    INCREMENT 1;

CREATE TABLE IF NOT EXISTS public.tenants (
    id            BIGINT PRIMARY KEY DEFAULT nextval('public.tenant_seq'),
    name          VARCHAR(100) NOT NULL UNIQUE,
    schema_name   VARCHAR(63)  NOT NULL UNIQUE,
    plan          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    contact_email VARCHAR(255) NOT NULL
);

-- index on schema_name - it's the lookup key on every request
CREATE INDEX IF NOT EXISTS idx_tenants_schema_name ON public.tenants(schema_name);
