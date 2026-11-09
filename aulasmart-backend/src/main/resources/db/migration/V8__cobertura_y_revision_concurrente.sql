ALTER TABLE sincronizacion_siga
    ADD COLUMN cobertura_inicio TIMESTAMPTZ,
    ADD COLUMN cobertura_fin TIMESTAMPTZ,
    ADD CONSTRAINT sincronizacion_cobertura_ck CHECK (
        (cobertura_inicio IS NULL AND cobertura_fin IS NULL)
        OR (cobertura_inicio IS NOT NULL AND cobertura_fin IS NOT NULL AND cobertura_fin > cobertura_inicio)
    );

ALTER TABLE solicitud_aula ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
