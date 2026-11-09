-- No decidir privilegios automáticamente. Flyway ejecuta la migración en una transacción.
CREATE TABLE IF NOT EXISTS usuario_rol_resolucion_v9 (
    id_usuario BIGINT PRIMARY KEY REFERENCES usuario(id_usuario),
    id_rol SMALLINT NOT NULL REFERENCES rol(id_rol),
    motivo TEXT NOT NULL CHECK (length(btrim(motivo)) > 0)
);

DO $$
DECLARE ambiguos INTEGER;
BEGIN
    SELECT count(*) INTO ambiguos FROM (
        SELECT u.id_usuario
        FROM usuario u LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
        LEFT JOIN rol r ON r.id_rol = ur.id_rol
        LEFT JOIN usuario_rol_resolucion_v9 res ON res.id_usuario = u.id_usuario
        GROUP BY u.id_usuario, res.id_rol
        HAVING res.id_rol IS NULL AND (count(ur.id_rol) <> 1
            OR bool_or(r.nombre NOT IN ('ADMINISTRADOR','ADMINISTRATIVO','DOCENTE','ESTUDIANTE')))
    ) pendientes;
    IF ambiguos > 0 THEN
        RAISE EXCEPTION 'Rol unico: % usuarios requieren asignacion explicita. Ejecute el preflight y apruebe un mapeo antes de reintentar.', ambiguos;
    END IF;
END $$;

ALTER TABLE usuario ADD COLUMN id_rol SMALLINT;
UPDATE usuario u SET id_rol = COALESCE(
    (SELECT res.id_rol FROM usuario_rol_resolucion_v9 res WHERE res.id_usuario = u.id_usuario),
    (SELECT min(ur.id_rol) FROM usuario_rol ur WHERE ur.id_usuario = u.id_usuario)
);
ALTER TABLE usuario ALTER COLUMN id_rol SET NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT usuario_rol_unico_fk FOREIGN KEY (id_rol) REFERENCES rol(id_rol);
ALTER TABLE rol ADD CONSTRAINT rol_catalogo_ck CHECK (nombre IN ('ADMINISTRADOR','ADMINISTRATIVO','DOCENTE','ESTUDIANTE'));
CREATE INDEX usuario_rol_activo_idx ON usuario(id_rol) WHERE activo;

-- Fotografía de las asociaciones anteriores: deja de participar en la autorización.
ALTER TABLE usuario_rol RENAME TO usuario_rol_historial_v9;
COMMENT ON TABLE usuario_rol_historial_v9 IS 'Historial previo a V9; el rol vigente esta en usuario.id_rol.';
