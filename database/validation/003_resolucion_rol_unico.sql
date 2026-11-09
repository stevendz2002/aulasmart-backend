-- Plantilla para ejecutar SOLO tras aprobar un mapeo de los usuarios del preflight.
-- No modifica asociaciones previas: V9 las conserva completas en el historial.
CREATE TABLE IF NOT EXISTS usuario_rol_resolucion_v9 (
    id_usuario BIGINT PRIMARY KEY REFERENCES usuario(id_usuario),
    id_rol SMALLINT NOT NULL REFERENCES rol(id_rol),
    motivo TEXT NOT NULL CHECK (length(btrim(motivo)) > 0)
);
-- Agregar INSERT con id_usuario, id_rol y motivo de cada decisión aprobada.
-- No asignar por prioridad de privilegios ni usar valores por defecto.
-- Una vez registrados los casos ambiguos, reintentar Flyway V9.
INSERT INTO usuario_rol_resolucion_v9
    (id_usuario, id_rol, motivo)
SELECT
    u.id_usuario,
    r.id_rol,
    'Cuenta mock: asignacion manual de rol unico DOCENTE'
FROM usuario u
CROSS JOIN rol r
WHERE u.id_usuario IN (9004, 9006)
  AND r.nombre = 'DOCENTE';