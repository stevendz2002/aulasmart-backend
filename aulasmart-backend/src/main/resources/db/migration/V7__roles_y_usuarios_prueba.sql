-- Siembra automática de roles del sistema y cuentas sintéticas de prueba para
-- permitir el acceso y evaluación inmediata en instancias y contenedores nuevos.
-- La migración es completamente idempotente.
BEGIN;

-- 1. Catálogo base de roles
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    (1, 'ADMINISTRADOR', 'PRUEBA: administra usuarios, roles y operación.'),
    (2, 'ADMINISTRATIVO', 'PRUEBA: revisa solicitudes y gestiona información operativa.'),
    (3, 'DOCENTE', 'PRUEBA: consulta aulas y registra solicitudes.'),
    (4, 'ESTUDIANTE', 'PRUEBA: consulta información de aulas.')
ON CONFLICT (id_rol) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion;

-- 2. Cuentas de prueba con BCrypt ('password')
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE LOWER(correo) = 'docente@prueba.aulasmart.local') THEN
        INSERT INTO usuario (id_usuario, nombre, correo, password_hash, activo)
        VALUES (9001, 'PRUEBA Docente', 'docente@prueba.aulasmart.local',
                '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE)
        ON CONFLICT (id_usuario) DO NOTHING;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE LOWER(correo) = 'revisor@prueba.aulasmart.local') THEN
        INSERT INTO usuario (id_usuario, nombre, correo, password_hash, activo)
        VALUES (9002, 'PRUEBA Revisor', 'revisor@prueba.aulasmart.local',
                '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE)
        ON CONFLICT (id_usuario) DO NOTHING;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE LOWER(correo) = 'admin@prueba.aulasmart.local') THEN
        INSERT INTO usuario (id_usuario, nombre, correo, password_hash, activo)
        VALUES (9003, 'PRUEBA Administrador', 'admin@prueba.aulasmart.local',
                '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE)
        ON CONFLICT (id_usuario) DO NOTHING;
    END IF;
END $$;

-- 3. Asignación de roles
INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuario u
CROSS JOIN rol r
WHERE LOWER(u.correo) = 'docente@prueba.aulasmart.local' AND r.nombre = 'DOCENTE'
ON CONFLICT (id_usuario, id_rol) DO NOTHING;

INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuario u
CROSS JOIN rol r
WHERE LOWER(u.correo) = 'revisor@prueba.aulasmart.local' AND r.nombre = 'ADMINISTRATIVO'
ON CONFLICT (id_usuario, id_rol) DO NOTHING;

INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuario u
CROSS JOIN rol r
WHERE LOWER(u.correo) = 'admin@prueba.aulasmart.local' AND r.nombre = 'ADMINISTRADOR'
ON CONFLICT (id_usuario, id_rol) DO NOTHING;

-- 4. Reajuste de secuencias de identidad
SELECT setval(
    pg_get_serial_sequence('usuario', 'id_usuario'),
    (SELECT COALESCE(MAX(id_usuario) + 1, 1) FROM usuario),
    false
);

SELECT setval(
    pg_get_serial_sequence('rol', 'id_rol'),
    (SELECT COALESCE(MAX(id_rol) + 1, 1) FROM rol),
    false
);

COMMIT;

