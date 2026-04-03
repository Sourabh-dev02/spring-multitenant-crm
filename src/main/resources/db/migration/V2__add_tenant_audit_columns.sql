-- V2__add_tenant_audit_columns.sql
-- Added after the first version went in - shows how Flyway handles incremental changes.
-- Also a good chance to add a created_at column we forgot in V1.

ALTER TABLE public.tenants
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ DEFAULT NOW();
