-- DATOS COMPLETAMENTE SINTÉTICOS. No provienen de SIGA ni representan personas,
-- aulas o eventos institucionales reales. Usar únicamente en aulasmart_test.
BEGIN;

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    (1, 'ADMINISTRADOR', 'PRUEBA: administra usuarios, roles y operación.'),
    (2, 'ADMINISTRATIVO', 'PRUEBA: revisa solicitudes y gestiona información operativa.'),
    (3, 'DOCENTE', 'PRUEBA: consulta aulas y registra solicitudes.'),
    (4, 'ESTUDIANTE', 'PRUEBA: consulta información de aulas.');

-- Hash BCrypt público de prueba, no una credencial institucional ni de producción.
INSERT INTO usuario (id_usuario, nombre, correo, password_hash, activo) VALUES
    (9001, 'PRUEBA Docente', 'docente@prueba.aulasmart.local',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
    (9002, 'PRUEBA Revisor', 'revisor@prueba.aulasmart.local',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE);

INSERT INTO usuario_rol (id_usuario, id_rol) VALUES
    (9001, 3),
    (9002, 2);

-- Cuenta del administrador (semilla de prueba): el único rol que crea cuentas.
INSERT INTO usuario (id_usuario, nombre, correo, password_hash, activo) VALUES
    (9003, 'PRUEBA Administrador', 'admin@prueba.aulasmart.local',
     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE);

INSERT INTO usuario_rol (id_usuario, id_rol) VALUES
    (9003, 1);

INSERT INTO edificio (id_edificio, codigo_siga, nombre) VALUES
    (9001, -9001, 'PRUEBA Edificio Norte'),
    (9002, -9002, 'PRUEBA Edificio Sur');

INSERT INTO tipo_aula (id_tipo_aula, codigo_siga, nombre) VALUES
    (9001, -9001, 'PRUEBA Salón'),
    (9002, -9002, 'PRUEBA Laboratorio');

INSERT INTO dependencia (id_dependencia, codigo_siga, nombre) VALUES
    (9001, -9001, 'PRUEBA Dependencia Académica');

INSERT INTO aula (
    id_aula, codigo_siga, nombre, id_edificio, capacidad,
    id_tipo_aula, id_dependencia, activa
) VALUES
    (9001, -9001, 'PRUEBA Aula 101', 9001, 40, 9001, 9001, TRUE),
    (9002, -9002, 'PRUEBA Aula 102', 9001, 30, 9001, 9001, TRUE),
    (9003, -9003, 'PRUEBA Laboratorio 201', 9002, 25, 9002, 9001, FALSE),
    (9004, -9004, 'PRUEBA Aula 103', 9001, 18, 9001, 9001, TRUE),
    (9005, -9005, 'PRUEBA Laboratorio 202', 9002, 24, 9002, 9001, TRUE);

INSERT INTO solicitud_aula (
    id_solicitud, id_usuario, id_aula, fecha_inicio, fecha_fin,
    estado, motivo, revisada_por, revisada_en
) VALUES
    (9001, 9001, 9002, '2026-11-10 10:00:00-05', '2026-11-10 12:00:00-05',
     'APROBADA', 'PRUEBA: actividad académica aprobada.', 9002, '2026-10-29 09:00:00-05'),
    (9002, 9001, 9001, '2026-11-11 14:00:00-05', '2026-11-11 16:00:00-05',
     'PENDIENTE', 'PRUEBA: actividad pendiente de revisión.', NULL, NULL),
    (9003, 9001, 9002, '2026-11-12 14:00:00-05', '2026-11-12 16:00:00-05',
     'RECHAZADA', 'PRUEBA: actividad rechazada.', 9002, '2026-10-29 09:05:00-05');

-- Reservas APROBADAS relativas al día de siembra: permiten comprobar con la
-- API que el filtro horario de disponibilidad realmente excluye ocupadas
-- (semántica [inicio, fin); SIGA solo trae catálogo, la ocupación proviene
-- de reservas aprobadas).
INSERT INTO solicitud_aula (
    id_solicitud, id_usuario, id_aula, fecha_inicio, fecha_fin,
    estado, motivo, revisada_por, revisada_en
) VALUES
    (9101, 9001, 9001, (CURRENT_DATE + TIME '07:00') AT TIME ZONE 'America/Bogota',
     (CURRENT_DATE + TIME '11:30') AT TIME ZONE 'America/Bogota',
     'APROBADA', 'PRUEBA: clase de la mañana aprobada.', 9002, now() - INTERVAL '2 days'),
    (9102, 9001, 9004, (CURRENT_DATE + TIME '14:00') AT TIME ZONE 'America/Bogota',
     (CURRENT_DATE + TIME '18:30') AT TIME ZONE 'America/Bogota',
     'APROBADA', 'PRUEBA: monitoria de la tarde aprobada.', 9002, now() - INTERVAL '2 days'),
    (9103, 9001, 9005, (CURRENT_DATE + TIME '15:00') AT TIME ZONE 'America/Bogota',
     (CURRENT_DATE + TIME '19:00') AT TIME ZONE 'America/Bogota',
     'APROBADA', 'PRUEBA: laboratorio aprobado.', 9002, now() - INTERVAL '1 day'),
    (9104, 9001, 9002, (CURRENT_DATE + TIME '08:00') AT TIME ZONE 'America/Bogota',
     (CURRENT_DATE + TIME '12:00') AT TIME ZONE 'America/Bogota',
     'APROBADA', 'PRUEBA: clase de mañana siguiente aprobada.', 9002, now() - INTERVAL '1 day');

INSERT INTO sincronizacion_siga (
    id_sincronizacion, recurso, iniciada_en, finalizada_en, estado,
    registros_procesados, detalle_error
) VALUES
    (9001, 'AULAS', '2026-10-29 08:00:00-05', '2026-10-29 08:01:00-05',
     'EXITOSA', 3, NULL);

COMMIT;
