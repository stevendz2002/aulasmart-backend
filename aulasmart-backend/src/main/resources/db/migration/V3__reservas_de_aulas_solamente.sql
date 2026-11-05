-- V3 __ reservas de aulas solamente ---------------------------------------------------
-- AulaSmart es una web de reservas académicas de aulas: la tabla `horario_academico`
-- (import de eventos académicos SIGA) se elimina junto con su contenido, y el control
-- de sincronización queda con el único recurso del producto: AULAS.

BEGIN;

-- 1. Eliminar el concepto de eventos académicos (tabla y datos).
DELETE FROM horario_academico;
DROP TABLE horario_academico;

-- 2. Eliminar las corridas de sincronización del recurso EVENTOS.
DELETE FROM sincronizacion_siga WHERE recurso = 'EVENTOS';

-- 3. Las ventanas de cobertura de eventos dejaron de existir.
ALTER TABLE sincronizacion_siga DROP CONSTRAINT sincronizacion_ventana_ck;
ALTER TABLE sincronizacion_siga DROP COLUMN ventana_inicio;
ALTER TABLE sincronizacion_siga DROP COLUMN ventana_fin;

-- 4. El único recurso sincronizable es el catálogo de aulas.
ALTER TABLE sincronizacion_siga DROP CONSTRAINT sincronizacion_recurso_ck;
ALTER TABLE sincronizacion_siga
    ADD CONSTRAINT sincronizacion_recurso_ck CHECK (recurso IN ('AULAS'));

COMMIT;
