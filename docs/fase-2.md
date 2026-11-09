# Fase 2: rol único por usuario

Estado: implementada y validada localmente; aprobada por el usuario para publicación el 2026-11-09.
Revisión: 2026-11-08. Rama: `feature/single-user-role`, cambios sin commit sobre `7030f16`.
Fase 0 publicada en `origin/refactor/architecture`.

## Evidencia de partida (antes de implementar)

- `UsuarioEntity` usa `ManyToMany` y `usuario_rol`; perfiles y DTOs exponen `roles[]`.
- `GestionUsuariosService` acepta, normaliza y deduplica varios roles.
- Formularios de usuarios usan casillas; sesión, guards y sidebar consultan listas.
- Basic y autorización backend ya funcionan; no se necesita JWT para esta fase.
- Se conoce la estructura y las semillas, no la distribución de roles de una base real.

## Alcance aprobado e implementado

1. Consolidar la fase 0 con un commit autorizado y crear `feature/single-user-role`
   desde ese punto, para conservar separadas las dos fases.
2. Agregar una migración posterior a V8: `usuario.id_rol` obligatorio con clave
   foránea. Migrar automáticamente solo usuarios con exactamente un rol válido.
3. Si hay usuarios sin rol o con varios, detener la migración sin cambios parciales
   y exigir un mapeo explícito aprobado. Nunca elegir el mayor privilegio ni borrar usuarios.
4. Preservar las asociaciones anteriores como historial de migración; la aplicación
   dejará de leerlas y escribirlas. Documentar que ese historial no es estado vigente.
5. Cambiar dominio, puertos, adaptadores, entidades y DTOs a `rol` singular. Rechazar
   cuerpos heredados con `roles` para evitar aceptar silenciosamente un contrato ambiguo.
6. Actualizar creación/edición con selección única obligatoria; sesión y permisos
   consultan un solo rol del usuario. Una lista de roles permitidos para una ruta
   sigue siendo válida: no equivale a varios roles asignados a una cuenta.
7. Mantener la autorización actual: todos consultan aulas; estudiante no solicita;
   docente solicita; administrativo y administrador solicitan/revisan y sincronizan;
   únicamente administrador gestiona usuarios. No agregar operaciones nuevas de aulas.
8. Conservar protección contra autodesactivación y evitar cambios que dejen al
   sistema sin administrador activo, también ante modificaciones concurrentes.
9. Adaptar semillas de prueba, pruebas y documentación sin reescribir V1–V8.

## Riesgos y aceptación

- Cambio incompatible de contrato: desplegar backend y frontend coordinadamente.
- Migración ambigua: probar que aborta atómicamente sin alterar usuarios.
- Acceso administrativo: probar cambios de rol, cuenta desactivada y concurrencia.
- Base: exactamente un rol válido por usuario, incluso al intentar escrituras SQL inválidas.
- UI/API: rechazar cero o varios roles; persistir correctamente creación y edición.
- Seguridad: verificar los cuatro roles en backend y navegación.
- Ejecutar migraciones sobre PostgreSQL descartable, suite backend/frontend y builds.
- Actualizar documentos al cierre y no avanzar a fase 1 sin autorización.

Estimación inicial: 3–5 jornadas. La implementación quedó aislada en su rama.

## Validación ejecutada

- Backend: `mvnw.cmd -o verify` con PostgreSQL 17 local descartable: **95 pruebas**, cero fallos, errores u omisiones; paquete generado.
- Incluye 12 pruebas de integración: migración inicial y desde V7, rechazo atómico
  de cuentas sin rol/múltiples, resolución explícita con historial preservado,
  restricciones de base, API singular, permisos y concurrencia.
- Se corrigió la distinción entre omitir `rol` en PATCH (conservarlo) y enviar
  `null` (rechazarlo). La protección administrativa excluye cuentas de prueba
  cuando están deshabilitadas por configuración; la prueba fija esa configuración.
- Frontend: `npm test -- --watch=false`: **62 pruebas en 17 archivos**, todas pasan.
  Incluye formularios con selección única y actualización de la sesión propia.
- `npm run build`: correcto. Advertencia preexistente de CSS de consulta:
  8,42 kB frente a presupuesto de 6 kB. No se realizó una inspección visual manual.
- Las pruebas solo modificaron una base descartable; no se aplicó V9 a la base del usuario.

## Aplicación y prueba manual

1. Respaldar la base y ejecutar `database/validation/002_preflight_rol_unico.sql`
   contra el esquema V8 antes de actualizar la aplicación.
2. Si aparecen usuarios sin rol o con varios, acordar el rol de cada cuenta.
   Ejecutar `003_resolucion_rol_unico.sql` y registrar las asignaciones aprobadas
   en `usuario_rol_resolucion_v9`, indicando usuario, rol y motivo. El script no
   asigna privilegios. Volver a comprobar que todas las cuentas ambiguas tienen mapeo.
3. Arrancar el backend para aplicar V9 y desplegar el frontend de la misma versión.
   No reutilizar el frontend antiguo: `roles[]` dejó de ser un contrato válido.
4. Iniciar sesión como administrador y probar creación/edición con un solo rol,
   cambio de permisos y rechazo de desactivación propia o pérdida del último administrador.
5. Probar docente, administrativo y estudiante con los permisos del alcance aprobado.
   Las cuentas de prueba conservan sus credenciales y requieren la opción de desarrollo
   `AULASMART_ALLOW_TEST_ACCOUNTS=true`. No se modificó el archivo local de configuración.

V9 conserva las asociaciones originales en `usuario_rol_historial_v9`, incluso
cuando se usa un mapeo explícito. El rol vigente es únicamente `usuario.id_rol`.
No hay reversión automática a la versión anterior: el cambio de contrato y esquema
requiere una estrategia coordinada con respaldo si se necesita retroceder.

## Pendientes

- El 2026-11-09 el usuario confirmó que migró los datos y no existen conflictos.
  Es confirmación del usuario; no se inspeccionó su base de datos.
- Publicación autorizada en `feature/single-user-role`; fase 1 autorizada a continuación.
- Fases 3 y 4 pendientes de autorización.
