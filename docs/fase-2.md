# Fase 2: rol único por usuario

Estado: implementación autorizada por el usuario; pendiente de ejecución y validación.
Revisión: 2026-11-08. Rama actual: `refactor/architecture`; fase 0 sin commit.

## Evidencia actual

- `UsuarioEntity` usa `ManyToMany` y `usuario_rol`; perfiles y DTOs exponen `roles[]`.
- `GestionUsuariosService` acepta, normaliza y deduplica varios roles.
- Formularios de usuarios usan casillas; sesión, guards y sidebar consultan listas.
- Basic y autorización backend ya funcionan; no se necesita JWT para esta fase.
- Se conoce la estructura y las semillas, no la distribución de roles de una base real.

## Implementación propuesta

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

Estimación inicial: 3–5 jornadas. No se crearon ramas ni commits durante el análisis.
