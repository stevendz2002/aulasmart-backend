-- Solo lectura. Ejecutar ANTES de V9 contra la base que se quiere migrar.
SELECT u.id_usuario, u.correo, u.activo, count(ur.id_rol) AS cantidad_roles,
       array_agg(r.nombre ORDER BY r.nombre) FILTER (WHERE r.id_rol IS NOT NULL) AS roles_actuales
FROM usuario u
LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
LEFT JOIN rol r ON r.id_rol = ur.id_rol
GROUP BY u.id_usuario, u.correo, u.activo
HAVING count(ur.id_rol) <> 1
    OR bool_or(r.nombre NOT IN ('ADMINISTRADOR','ADMINISTRATIVO','DOCENTE','ESTUDIANTE'))
ORDER BY u.id_usuario;
