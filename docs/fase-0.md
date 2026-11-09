# Fase 0: auditoría y estabilización

Revisión: 2026-11-08. Rama `refactor/architecture`. Base `4cd75fc`.
Estado: fase 0 implementada y validada en desarrollo. Sin commits ni despliegue.

## Hallazgos y cambios

- CORS omitía PATCH: se habilitó con prueba de preflight y rechazo de origen externo.
- SIGA fallido equivalía a ausencia de eventos: ahora interrumpe registro/aprobación
  con disponibilidad no verificable (503). Código SIGA nulo/cero tampoco confirma disponibilidad.
- Se aprobaban aulas sin resultado activo: ahora se exige aula activa.
- La búsqueda validaba antigüedad pero no cobertura: V8 registra la ventana real
  y exige que cubra todo el intervalo; la última corrida debe ser exitosa y posterior al catálogo.
- El detalle omitía reservas locales: ahora reúne ocupación SIGA y solicitudes aprobadas.
- La interfaz convertía errores en horarios libres: se separó el error del vacío válido,
  se descartan respuestas atrasadas y disponibilidad anterior cuando una nueva búsqueda falla.
- Las fechas de consulta/solicitud y los horarios del detalle se interpretan en Colombia.
- Las revisiones podían sobrescribirse: versionado optimista y traducción de conflictos a 409.
- Se conservó EXCLUDE en PostgreSQL y se agregó prueba de dos aprobaciones solapadas.
- Basic se guardaba en sessionStorage: permanece solo en memoria; recargar exige login.
- Las cuentas de prueba funcionaban por defecto: ahora requieren activación explícita.
- El interceptor adjuntaba credenciales sin limitar destino: ahora solo a la API de AulaSmart.
- Sincronizadores acoplados al cliente HTTP: ahora dependen de `ConsultaSigaPort`.
- Aplicación acoplada a Authentication: ahora la presentación entrega el correo.

## Validaciones registradas

- Línea base: 63 pruebas backend y 51 frontend aprobadas.
- Primer bloque: 73 pruebas backend y 51 frontend aprobadas; compilación correcta.
- Ejecución intermedia ampliada: 82 pruebas backend aprobadas, incluidas 3 con
  PostgreSQL 17.4 real: migraciones V1–V8, revisión concurrente y exclusión de solapes.
- Ejecución intermedia frontend: 55 pruebas aprobadas, incluidas pruebas de estados
  de ocupación, fechas y descarte de respuestas atrasadas.
- Validación final `mvnw.cmd -o verify`: **86 pruebas aprobadas**, sin errores ni
  omisiones, y paquete ejecutable generado. Incluye **6 pruebas con PostgreSQL 17.4**:
  migraciones iniciales, actualización V7→V8 sin pérdida de usuarios, versión de
  revisión, dos escenarios concurrentes y autorización HTTP (agrupados en seis casos).
- Validación final `npm test -- --watch=false`: **60 pruebas aprobadas en 16 archivos**.
  Incluye destinos del interceptor, sesión en memoria, estados de error, respuestas
  atrasadas, descarte de disponibilidad anterior y horarios en Colombia.
- `npm run build`: correcto. Advertencia previa de `consulta-page.css`: 8,42 kB
  frente a presupuesto de 6 kB; no impide compilar y queda para fase 3.
- Un intento fue impedido por límite de uso del revisor de permisos; el reintento
  posterior se ejecutó correctamente. PostgreSQL temporal fue detenido al terminar.
- No se modificó la base habitual del usuario ni se consultó SIGA real durante estas pruebas.

## Criterios de cierre

- [x] Hallazgos y decisiones documentados con alcance explícito.
- [x] Sincronización fallida no registra/aprueba una solicitud.
- [x] Aprobación requiere aula activa.
- [x] Pruebas reales de concurrencia en PostgreSQL y migración inicial V8.
- [x] Suite final, incluyendo actualización desde V7, aprobada sobre el estado final.
- [x] Compilación final backend/frontend y comprobación de diferencias.
- [x] Registrar limitaciones y detener PostgreSQL temporal al finalizar.

## Uso y operación

Al aplicar V8, ejecutar una sincronización de AULAS y después EVENTOS. Hasta
tener cobertura válida las consultas de disponibilidad devolverán 503.
Para cuentas sintéticas locales, activar explícitamente `AULASMART_ALLOW_TEST_ACCOUNTS=true`
en el entorno local y reiniciar backend. No habilitarlo para un despliegue real.
Las cuentas institucionales existentes no se eliminan ni cambian de rol.

Pruebas habituales: `mvnw.cmd test` en backend; `npm test -- --watch=false` y
`npm run build` en frontend. Para integración, usar una base PostgreSQL descartable
con usuario `audit` y establecer `AULASMART_TEST_DB_URL` antes de `mvnw.cmd verify`.
`Phase0PostgresTest` crea datos sintéticos y aplica migraciones: nunca apuntarlo a una base del usuario.
Sin esa variable, sus casos se omiten; no afirmar validación PostgreSQL si se omitieron.

## Pendientes fuera de esta fase

Rol único, consolidación funcional, rediseño y JWT permanecen sujetos a aprobación.
No se verificaron capacidades de escritura institucional ni se hicieron reservas en SIGA.
Las limitaciones de despliegue y seguridad transitoria están en [arquitectura](arquitectura.md).
