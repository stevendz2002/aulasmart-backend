# Backend inicial: estado verificado

Fecha de verificación: **29 de octubre de 2026**, en la rama `feat/backend-inicial`. Todos los resultados de este documento provienen de ejecuciones reales sobre el contenedor `aulasmart-postgres-test` y la base `aulasmart_test`.

## Alcance implementado

Un único despliegue Spring Boot 3.5.7 con Java 17, construido con Maven Wrapper (no requiere Maven instalado) y organizado como monolito modular en `co.edu.uceva.aulasmart`:

- `aulas`: dominio (`Aula`, `AulaFiltro`, `Intervalo`, `Disponibilidad`, `CatalogoAula`), excepciones de dominio, servicios de consulta y disponibilidad, puertos y adaptadores de persistencia, y API REST.
- `users`: entidades `Usuario` y `Rol`, `JpaUserDetailsService`, y seguridad con HTTP Basic y roles leídos de la base.
- `solicitudes`: adaptador de lectura de solicitudes aprobadas para las reglas de disponibilidad. No expone todavía la API de creación y revisión.
- `shared`: reloj UTC configurable, manejo de errores con `ApiError` y mapeo de excepciones de dominio a códigos HTTP.

**No incluye:** sincronización programada con SIGA, creación y revisión de solicitudes por API, frontend Angular ni AulaBot. La evidencia de sincronización proviene todavía de la semilla sintética.

## Construcción y pruebas unitarias

Desde `aulasmart-backend`:

```powershell
.\mvnw.cmd -B -ntp test
```

Resultado real: `BUILD SUCCESS`, **8 pruebas, 0 fallos, 0 errores**:

| Clase de prueba | Pruebas |
|---|---|
| `DisponibilidadAulaServiceTest` | 3 |
| `IntervaloTest` | 3 |
| `AulaFiltroTest` | 1 |
| `AulasmartBackendApplicationTests` | 1 |

La prueba de carga de contexto de la plantilla se sustituyó por una comprobación sin base de datos; los comportamientos del API se validaron en vivo con peticiones reales, como se documenta en las secciones siguientes.

## Arranque sobre la base existente

1. Contenedor saludable: `docker compose up -d` hasta obtener estado `healthy`.
2. Desde `aulasmart-backend`, exportar la credencial local y abrir el servicio:

```powershell
$env:DB_PASSWORD = '<valor de POSTGRES_PASSWORD del .env>'
.\mvnw.cmd -B -ntp spring-boot:run
```

Registro observado en ambas corridas:

- Flyway validó `V1__initial_schema.sql` sobre `aulasmart_test`, fijó el schema history con baseline en la **versión 1** y confirmó `Schema "public" is up to date`.
- Hibernate arrancó con `spring.jpa.hibernate.ddl-auto=validate` sin diferencias de mapeo contra la base existente.
- Aplicación iniciada en ~12 s; `GET /actuator/health` devolvió `{"status":"UP"}`.

## Seguridad y cuenta de prueba

`GET /api/aulas/**` exige uno de los roles de la base (`ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`). La autenticación es HTTP Basic contra `usuario` (correo como nombre de usuario y hash BCrypt). La semilla usa el hash BCrypt público al que corresponde la frase `password` para ambos usuarios sintéticos:

- `docente@prueba.aulasmart.local` — rol `DOCENTE`.
- `revisor@prueba.aulasmart.local` — rol `ADMINISTRATIVO`.

Son credenciales exclusivas de datos de prueba; no representan personas reales.

## Comportamiento comprobado con peticiones reales

| Petición | Resultado real |
|---|---|
| `GET /api/aulas` sin credenciales | `401` y cuerpo vacío |
| `GET /api/aulas` con docente de prueba | 200 con 2 aulas activas (`9001`, `9002`); la `9003` inactiva no aparece |
| `GET /api/aulas/9001` | 200 con edificio, tipo y dependencia |
| `GET /api/aulas/9003` | `404`: el aula inactiva no es consultable |
| `GET /api/aulas/disponibles?inicio=2026-11-10T08:00:00-05:00&fin=2026-11-10T10:00:00-05:00` | solo `9002`: el horario 08:00–10:00 excluye a `9001`; la solicitud aprobada de `9002` (10:00–12:00) no solapa bajo la semántica `[inicio, fin)` |
| `GET /api/aulas/disponibles?inicio=2026-11-10T12:00:00-05:00&fin=2026-11-10T14:00:00-05:00` | `9001` y `9002`, sin ocupaciones |
| Intervalo con `fin` anterior a `inicio` | `400` con `ApiError`: "La fecha de finalización debe ser posterior al inicio." |
| `GET /api/aulas/disponibles` sin parámetros | `400`: "Los parámetros de la consulta son inválidos o incompletos." |
| Consulta del 5 de enero de 2027, fuera de la ventana sincronizada de eventos | **`503`** con `ApiError`: "No se puede confirmar la disponibilidad porque la sincronización está incompleta, desactualizada o no cubre el intervalo consultado." |

Las respuestas de disponibilidad incluyen `verificadoEn` en UTC (instalación con reloj `Clock.systemUTC()`).

## Puerta de calidad de disponibilidad

El servicio de disponibilidad exige, antes de afirmar que un aula está libre:

1. Una sincronización de `AULAS` con estado `EXITOSA` no más antigua que la antigüedad máxima configurada (`aulasmart.disponibilidad.max-antiguedad`, PT24H por omisión).
2. Una sincronización de `EVENTOS` con estado `EXITOSA`, vigente en la misma antigüedad máxima, cuya ventana (`ventana_inicio`–`ventana_fin`) cubra las fechas del intervalo consultado en la zona `America/Bogota`.
3. Ausencia de solapamiento con horarios académicos y con solicitudes en estado `APROBADA`.

Si alguno falla, responde **503** en lugar de una lista de aulas: no se afirma disponibilidad sin evidencia vigente. Este comportamiento quedó comprobado con la consulta de 2027.

## Límites de esta verificación

- Sincronización con SIGA: solo el modelo de control (`sincronizacion_siga`) está implementado; el trabajo programado y el adaptador HTTP no existen todavía.
- Autenticación: solo HTTP Basic con roles; no hay registro de cuentas, JWT ni refresh.
- No se probó CORS desde un navegador ni despliegue en contenedor del propio backend.
- La conexión gráfica con pgAdmin sigue sin ejecutarse.
