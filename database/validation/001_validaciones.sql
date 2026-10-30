\set ON_ERROR_STOP on

\echo '1. Tablas y datos semánticos de prueba'
SELECT 'aula' AS entidad, COUNT(*) FROM aula
UNION ALL SELECT 'solicitud_aula', COUNT(*) FROM solicitud_aula
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'rol', COUNT(*) FROM rol
UNION ALL SELECT 'sincronizacion_siga', COUNT(*) FROM sincronizacion_siga;

\echo '2. Consulta por edificio y capacidad mínima (solo activas)'
SELECT a.id_aula, a.nombre, e.nombre AS edificio, a.capacidad, t.nombre AS tipo
FROM aula a
JOIN edificio e ON e.id_edificio = a.id_edificio
JOIN tipo_aula t ON t.id_tipo_aula = a.id_tipo_aula
WHERE a.activa AND e.id_edificio = 9001 AND a.capacidad >= 40
ORDER BY a.id_aula;

\echo '3. Disponibilidad: exige catálogo AULAS vigente y excluye reservas aprobadas solapadas'
WITH parametros AS (
    SELECT '2026-11-10 08:00:00-05'::timestamptz AS inicio,
           '2026-11-10 10:00:00-05'::timestamptz AS fin
), sincronizacion_vigente AS (
    SELECT EXISTS (
        SELECT 1 FROM sincronizacion_siga
        WHERE recurso = 'AULAS' AND estado = 'EXITOSA'
    ) AS valida
)
SELECT a.id_aula, a.nombre
FROM aula a
CROSS JOIN parametros p
CROSS JOIN sincronizacion_vigente sv
WHERE sv.valida
  AND a.activa
  AND a.capacidad >= 30
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_aula s
      WHERE s.id_aula = a.id_aula AND s.estado = 'APROBADA'
        AND s.fecha_inicio < p.fin AND s.fecha_fin > p.inicio
  )
ORDER BY a.id_aula;

\echo '4. Las ocupaciones provienen solo de reservas aprobadas'
SELECT s.id_solicitud, s.id_aula, s.fecha_inicio, s.fecha_fin, s.estado
FROM solicitud_aula s
WHERE s.id_aula = 9002
  AND s.estado = 'APROBADA'
  AND s.fecha_inicio < '2026-11-10 13:00:00-05'
  AND s.fecha_fin > '2026-11-10 10:00:00-05';

\echo '5. No existen referencias huérfanas'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM aula a LEFT JOIN edificio e USING (id_edificio) WHERE e.id_edificio IS NULL)
       OR EXISTS (SELECT 1 FROM solicitud_aula s LEFT JOIN usuario u ON u.id_usuario = s.id_usuario WHERE u.id_usuario IS NULL) THEN
        RAISE EXCEPTION 'Se encontraron referencias huérfanas';
    END IF;
    RAISE NOTICE 'OK: integridad referencial verificada';
END $$;

\echo '6. La base impide solicitudes aprobadas solapadas'
DO $$
BEGIN
    BEGIN
        INSERT INTO solicitud_aula (
            id_solicitud, id_usuario, id_aula, fecha_inicio, fecha_fin,
            estado, motivo, revisada_por, revisada_en
        ) VALUES (
            9901, 9001, 9002, '2026-11-10 11:00:00-05', '2026-11-10 13:00:00-05',
            'APROBADA', 'PRUEBA: debe fallar por solapamiento.', 9002, CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'La restricción de solapamiento no funcionó';
    EXCEPTION WHEN exclusion_violation THEN
        RAISE NOTICE 'OK: solapamiento aprobado rechazado';
    END;
END $$;

\echo '7. La base impide duplicar códigos externos'
DO $$
BEGIN
    BEGIN
        INSERT INTO edificio (codigo_siga, nombre) VALUES (-9001, 'PRUEBA duplicado');
        RAISE EXCEPTION 'La restricción de código único no funcionó';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'OK: código externo duplicado rechazado';
    END;
END $$;

\echo '8. Se validan intervalos y auditoría de revisión'
DO $$
BEGIN
    BEGIN
        INSERT INTO solicitud_aula (id_usuario, id_aula, fecha_inicio, fecha_fin, motivo)
        VALUES (9001, 9001, '2026-11-10 10:00:00-05', '2026-11-10 09:00:00-05', 'PRUEBA');
        RAISE EXCEPTION 'La validación de intervalo no funcionó';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK: intervalo inválido rechazado';
    END;

    BEGIN
        INSERT INTO solicitud_aula (
            id_usuario, id_aula, fecha_inicio, fecha_fin, estado, motivo
        ) VALUES (
            9001, 9001, '2026-11-15 10:00:00-05', '2026-11-15 11:00:00-05',
            'APROBADA', 'PRUEBA sin revisor'
        );
        RAISE EXCEPTION 'La validación de auditoría no funcionó';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK: aprobación sin auditoría rechazada';
    END;
END $$;

\echo '9. El único recurso de sincronización es AULAS'
DO $$
BEGIN
    BEGIN
        INSERT INTO sincronizacion_siga (recurso, estado, registros_procesados)
        VALUES ('EVENTOS', 'EN_CURSO', 0);
        RAISE EXCEPTION 'El recurso EVENTOS sigue aceptado';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK: recurso EVENTOS rechazado';
    END;
END $$;

\echo 'TODAS LAS VALIDACIONES FINALIZARON SIN ERRORES'
