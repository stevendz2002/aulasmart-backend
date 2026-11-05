-- Detalle textual de la ocupación (título del evento SIGA) para la ficha
-- del aula. Idempotente: las bases que ya tengan la columna no se alteran.
BEGIN;

ALTER TABLE horario_academico
    ADD COLUMN IF NOT EXISTS detalles VARCHAR(200);

COMMIT;
