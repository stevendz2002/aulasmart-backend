\set ON_ERROR_STOP on

\echo '1. Tablas y datos semánticos de prueba'
SELECT 'aula' AS entidad, COUNT(*) FROM aula
UNION ALL SELECT 'horario_academico', COUNT(*) FROM horario_academico
UNION ALL SELECT 'solicitud_aula', COUNT(*) FROM solicitud_aula
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'rol', COUNT(*) FROM rol;

\echo '2. Consulta por edificio y capacidad mínima (solo activas)'
SELECT a.id_aula, a.nombre, e.nombre AS edificio, a.capacidad, t.nombre AS tipo
FROM aula a
JOIN edificio e ON e.id_edificio = a.id_edificio
JOIN tipo_aula t ON t.id_tipo_aula = a.id_tipo_aula
WHERE a.activa AND e.id_edificio = 9001 AND a.capacidad >= 40
ORDER BY a.id_aula;

\echo '3. Disponibilidad: excluye eventos y solicitudes aprobadas solapados'
WITH parametros AS (
    SELECT '2026-11-10 08:00:00-05'::timestamptz AS inicio,
           '2026-11-10 10:00:00-05'::timestamptz AS fin
), sincronizacion_completa AS (
    SELECT
        EXISTS (
            SELECT 1 FROM sincronizacion_siga
            WHERE recurso = 'AULAS' AND estado = 'EXITOSA'
        ) AND EXISTS (
            SELECT 1 FROM sincronizacion_siga s, parametros p
            WHERE s.recurso = 'EVENTOS' AND s.estado = 'EXITOSA'
              AND s.ventana_inicio <= p.inicio::date
              AND s.ventana_fin >= p.fin::date
        ) AS valida
)
SELECT a.id_aula, a.nombre
FROM aula a
CROSS JOIN parametros p
CROSS JOIN sincronizacion_completa sc
WHERE sc.valida
  AND a.activa
  AND a.capacidad >= 30
  AND NOT EXISTS (
      SELECT 1 FROM horario_academico h
      WHERE h.id_aula = a.id_aula
        AND h.fecha_inicio < p.fin AND h.fecha_fin > p.inicio
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_aula s
      WHERE s.id_aula = a.id_aula AND s.estado = 'APROBADA'
        AND s.fecha_inicio < p.fin AND s.fecha_fin > p.inicio
  )
ORDER BY a.id_aula;

\echo '4. Ocupaciones que explican el conflicto del aula 9001'
SELECT h.id_horario, h.id_aula, h.fecha_inicio, h.fecha_fin, h.fuente
FROM horario_academico h
WHERE h.id_aula = 9001
  AND h.fecha_inicio < '2026-11-10 10:00:00-05'
  AND h.fecha_fin > '2026-11-10 08:00:00-05';

\echo '5. No existen referencias huérfanas'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM aula a LEFT JOIN edificio e USING (id_edificio) WHERE e.id_edificio IS NULL)
       OR EXISTS (SELECT 1 FROM horario_academico h LEFT JOIN aula a USING (id_aula) WHERE a.id_aula IS NULL)
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

\echo 'TODAS LAS VALIDACIONES FINALIZARON SIN ERRORES'
