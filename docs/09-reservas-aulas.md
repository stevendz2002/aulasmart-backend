# Reservas de aulas: estado verificado

Fecha de verificación: **29 de octubre de 2026**, sobre la base `aulasmart_test` (contenedor `aulasmart-postgres-test`, PostgreSQL 17.4) y arranques reales del backend Spring Boot. Todos los resultados provienen de ejecuciones reales: la suite de pruebas Maven, migración Flyway aplicada por el backend en arranque real, y llamadas HTTP reales al API propio.

## Decisión de producto

AulaSmart es una **web de reservas académicas de aulas**. El alcance quedó delimitado en esta fase:

- **Las reservas solo son de aulas.** Todo lo relativo a eventos se eliminó del producto: código, esquema y datos.
- **Solo `DOCENTE` crea solicitudes**; las revisan `ADMINISTRATIVO` y `ADMINISTRADOR` (que además consultan todas; el docente consulta solo las suyas).
- Las ocupaciones provienen exclusivamente de las solicitudes en estado `APROBADA`: la base de datos es la única fuente de confinamiento de aulas.

## Purga de eventos

### Migración `V3__reservas_de_aulas_solamente.sql`

```sql
DELETE FROM horario_academico;  DROP TABLE horario_academico;
DELETE FROM sincronizacion_siga WHERE recurso = 'EVENTOS';
ALTER TABLE sincronizacion_siga DROP CONSTRAINT sincronizacion_ventana_ck;
ALTER TABLE sincronizacion_siga DROP COLUMN ventana_inicio;
ALTER TABLE sincronizacion_siga DROP COLUMN ventana_fin;
ALTER TABLE sincronizacion_siga DROP CONSTRAINT sincronizacion_recurso_ck;
ALTER TABLE sincronizacion_siga
    ADD CONSTRAINT sincronizacion_recurso_ck CHECK (recurso IN ('AULAS'));
```

### Evidencias reales de la aplicación de V3

El backend la aplicó en arranque real (Flyway: `Migrating schema "public" to version "3 - reservas de aulas solamente"`). Estado medido con `psql` tras el arranque:

| Comprobación | Resultado |
|---|---|
| Filas `EVENTOS` en `sincronizacion_siga` | `0` (purgadas las 3 existentes: semilla y 2 reales) |
| Columnas `ventana*` en la matriz de `sincronizacion_siga` | ausentes (`id_sincronizacion, recurso, iniciada_en, finalizada_en, estado, registros_procesados, detalle_error`) |
| Tabla `horario_academico` | no existe (`0` filas en `information_schema.tables`) |
| Historial Flyway | `3 · reservas de aulas solamente` |

### Código eliminado o reescrito

- Fuera del proyecto: `SincronizadorDeEventosService` (y su prueba), `SigaEventosRespuesta`, `HorarioAcademicoPort`, `HorarioAcademicoEntity`, su `JpaRepository` y su adaptador.
- Reescritos para solo `AULAS`: `SincronizacionSigaService`, `TareaSincronizacionProgramada`, `SincronizacionController`, `SigaRestClient`, `SigaPropiedades` (sin `Eventos`), `SincronizacionSigaPort` (ahora `catalogoAulasVigente`), `RegistroSincronizacionPort.registrarInicio(String recurso)` (sin ventana), entidad y repositorio de control, y `DisponibilidadAulaService` (excluye solo reservas APROBADAS).
- Propiedad `aulasmart.siga.eventos.*` eliminada de `application.properties`.
- Semillas (`database/seeds/001_datos_prueba.sql`) sin fila de horarios ni de `EVENTOS`; `database/validation/001_validaciones.sql` al mundo de reservas (con sección nueva: la base rechaza insertar recurso `EVENTOS`).

## Diseño de la API de solicitudes

Módulo nuevo `solicitudes` con la misma estructura hexagonal del resto del monolito:

- **Dominio:** `SolicitudReserva` con estados `PENDIENTE / APROBADA / RECHAZADA` y las excepciones `SolicitudNoEncontradaException`, `SolicitudYaRevisadaException` y `ReservaSolapadaException`.
- **Aplicación:** `RegistrarSolicitudService` (motivo obligatorio, aula activa obligatoria, re-verificación de solape antes de guardar), `RevisarSolicitudService` (APROBAR/RECHAZAR con re-verificación de solape al aprobar), `ConsultarSolicitudesService` (docente: propias con filtro opcional; revisor: todas con filtro) y `PrincipalDeSolicitudes` (resuelve el usuario autenticado por correo y su rol).
- **Puertos:** `SolicitudReservaPort` (guardar y consultas) con adaptador y mapeador propios en la capa de persistencia; `ConsultaUsuariosPort` nuevo en `users` para resolver el principal; reutilización de `AulaConsultaPort` y `SolicitudAprobadaPort` de `aulas`.
- **Presentación:** `POST /api/solicitudes` con `{aulaId, inicio, fin, motivo}` → `201`; `GET /api/solicitudes?estado=` → lista; `POST /api/solicitudes/{id}/revision` con `{decision: APROBAR|RECHAZAR}` → `200`. Respuestas `404/409/400` por `RestExceptionHandler`.
- **Seguridad:** `POST /api/solicitudes` solo `DOCENTE`; `GET` solo `DOCENTE/ADMINISTRATIVO/ADMINISTRADOR`; revisión solo `ADMINISTRATIVO/ADMINISTRADOR`. Como defensa adicional, el controlador reexige el rol del usuario autenticado registrado en la base.
- **Semántica del intervalo:** `[inicio, fin)` verificada en creación (solape con APROBADAS → `409`), en aprobación (re-verificación transaccional) y finalmente garantizada por la restricción EXCLUDE de `solicitud_aula`.

## Suite de pruebas (Maven)

27 pruebas, 0 fallas, 0 errores, build exitoso:

| Clase | Pruebas | Cubre |
|---|---|---|
| `DisponibilidadAulaServiceTest` | 3 | exclusión de reservas APROBADAS, puerta de calidad, sin ocupación |
| `AulaFiltroTest` / `IntervaloTest` | 1 / 3 | filtros e intervalos `[inicio, fin)` |
| `SincronizadorDeAulasServiceTest` | 3 | corrida EXITOSA/FALLIDA del catálogo |
| `SigaAulasRespuestaTest` | 5 | validación estricta de respuestas SIGA |
| `RegistrarSolicitudServiceTest` | 5 | 201 pendiente, motivo, aula inactiva/inexistente, solape |
| `RevisarSolicitudServiceTest` | 6 | aprobar/rechazar con auditoría, ya revisada, solape al aprobar, decisión inválida |
| `AulasmartBackendApplicationTests` | 1 | configuración mínima |

## Arranque real: detección de una falla fuera de la suite

La primera verificación viva falló: Spring Data no pudo crear `findByIdUsuarioOrderByCreadaEnDesc` porque la propiedad de la entidad es `docenteId` (columna `id_usuario`). Las pruebas no lo detectaron porque el único test de contexto hace reflexión, no arranca el contenedor; el arranque real sí. Se corrigió a `findByDocenteIdOrderByCreadaEnDesc`, se repitieron las 27 pruebas y el arranque, y la API quedó funcionando. Es la razón por la que las fases del proyecto siempre cierran con arranque y HTTP reales además de la suite.

## E2E de sincronización y disponibilidad (HTTP real)

| Ejecución | Resultado |
|---|---|
| `POST /api/sincronizaciones?recurso=EVENTOS` (ADMINISTRATIVO) | **400**: "Recurso de sincronización desconocido: `EVENTOS`. Valor de sincronización válido: AULAS." |
| `POST /api/sincronizaciones?recurso=AULAS` (ADMINISTRATIVO) | **200**: `EXITOSA`, `registrosProcesados: 104`, duración 3.05 s |
| `GET /api/aulas/disponibles` 13:00–15:00 UTC (fresco, capacidad ≥ 200) | **200**: 6 aulas (sede Cali 406, Calima 407, Coliseo, Cancha de Fútbol, LACII, Pista) |
| Misma consulta con la sincronización envejecida 48 h (simulada con `UPDATE` y luego restaurada) | **503**: "No se puede confirmar la disponibilidad porque la sincronización está incompleta, desactualizada o no cubre el intervalo consultado." |
| `001_validaciones.sql` sobre datos reales | 9 secciones OK: conteos (`aula=107`, `solicitud_aula=3`, `usuario=2`, `rol=4`, `sincronizacion_siga=5`), disponibilidad solo-APROBADAS sobre 98 aulas, EXCLUDE contra solapamiento, unicidad de códigos, validaciones de intervalos y auditoría, y rechazo de `EVENTOS` por `CHECK` |

## E2E de la API de solicitudes (HTTP real)

Solicitudes sembradas de referencia: `9001` APROBADA (aula 9002, 10:00–12:00 -05), `9002` PENDIENTE, `9003` RECHAZADA.

| Ejecución | Resultado |
|---|---|
| `GET` sin credenciales | **401** |
| `POST /api/solicitudes` como revisor | **403** |
| `POST /api/solicitudes` como DOCENTE (aula 9006, 13:00–15:00 UTC) | **201**: id 9008, `PENDIENTE`, `docenteId 9001`, motivo registrado |
| `POST` con motivo `"   "` | **400**: "El motivo de la solicitud es obligatorio." |
| `POST` con `aulaId 9999` | **404**: "No existe un aula activa con el identificador 9999." |
| `GET` como docente | **200**: las 3 propias (9001–9003), ninguna ajena |
| `GET ?estado=PENDIENTE` como revisor | **200**: solo la 9002 |
| `GET ?estado=APROBADA` como docente | **200**: la 9001 (semilla) y la 9008 (creada y aprobada en el E2E) |
| `GET ?estado=CANCELADA` | **400**: "Estado desconocido… Estados válidos: PENDIENTE, APROBADA, RECHAZADA." |
| `POST /9008/revision` como DOCENTE | **403** |
| `POST /9008/revision` APROBAR como revisor | **200**: `APROBADA`, `revisadaPorId 9002`, `revisadaEn` registrada |
| Reaprobar la 9008 | **409**: "La solicitud 9008 ya fue revisada (estado APROBADA)." |
| Crear solicitud solapada con la APROBADA de la 9008 (14:00–16:00) | **409**: "El aula 9006 ya tiene una reserva aprobada…" |
| Crear solicitud contigua `[15:00, 16:00)` en la misma aula | **201** (id 9009): la semántica `[inicio, fin)` permite intervalos que solo tocan el límite |
| `POST /9010/revision` RECHAZAR (solicitud de la 9007, 13:00–15:00) | **200**: `RECHAZADA` con auditoría del revisor |
| `POST /9010/revision` con `decision CANCELAR` | **400**: "La decisión de revisión debe ser APROBAR o RECHAZAR…" |
| `GET /api/aulas/disponibles` 13:00–15:00 UTC tras aprobar la 9008 | **200** con 105 aulas y **el aula 9006 ausente**: la disponibilidad integra las reservas aprobadas |

Tras terminar el E2E se restableció la base: las solicitudes de prueba creadas vía API (9008–9010) se eliminaron con `DELETE`, quedando únicamente las semillas.

## Estado y pendientes

- **No existe todavía:** frontend Angular, AulaBot, despliegue en producción, y paginación de la lista de solicitudes (convive con el tamaño actual probado).
- La integración con el ciclo de disponibilidad quedó comprobada; la puerta de calidad (antigüedad máxima 24 h) se mantiene.
