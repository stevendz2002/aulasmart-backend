# AulaSmart

AulaSmart es un sistema web académico de **reservas de aulas** para la Unidad Central del Valle del Cauca (UCEVA): los docentes registran solicitudes de reserva con semántica de intervalo medio abierto `[inicio, fin)`, el personal administrativo las revisa (aprobar/rechazar) y el catálogo de aulas se mantiene sincronizado desde el sistema SIGA de la universidad.

> Estado actual: fase de backend monolito modular **implementada y verificada** (27 pruebas Maven en verde y validaciones SQL automatizadas contra la base). En planeación: frontend SPA Angular y asistente conversacional AulaBot.

## Arquitectura

Backend único tipo **monolito modular** con Spring Boot 3.x (Java 17) organizado con Clean Architecture (`domain · application · infrastructure · presentation`), una única base PostgreSQL y una sola canalización de despliegue.

| Módulo | Responsabilidad | Estado |
|---|---|---|
| `users` | Cuentas, roles y seguridad (`ConsultaUsuariosPort` interno) | Verificado |
| `aulas` | API REST de catálogo y reglas de disponibilidad (puerta de calidad de sincronización ≤ 24 h) | Verificado |
| `solicitudes` | Registro, revisión y consulta de reservas con auditoría y re-verificación de solape | Verificado |
| `integrations.siga` | Sincronización del catálogo de aulas (manual y programada), transformación estricta y registro de corridas | Verificado |
| `persistencia` | Repositorios Spring Data JPA y migraciones Flyway compartidos | Verificado |
| Frontend Angular | SPA de consulta, solicitudes y autenticación | Planeado |
| `chatbot` + `integrations.ai` | AulaBot con proveedor de IA (API Gemini); puerto definido, adaptador pendiente | Planeado |

La decisión monolito modular frente a microservicios se revisará solo con evidencia de equipos autónomos, cargas de escalado distintas o requisitos de aislamiento; los puertos internos permiten extraer un componente sin reescribir el resto.

## Stack y roles

- **Java 17 / Spring Boot 3.5.7**: Web MVC, Security (autenticación por credenciales y autorización por rol), Validation, Data JPA (Hibernate), Actuator.
- **PostgreSQL 17.4** en Docker Compose (puerto 5433, base `aulasmart_test`), migraciones Flyway V1→V3.
- **Roles**: `ESTUDIANTE`, `DOCENTE`, `ADMINISTRATIVO`, `ADMINISTRADOR`. Solo el docente crea solicitudes; el administrativo/administrador las revisa y consulta todo.

La integridad de las reservas no depende de la aplicación sola: intervalos `TIMESTAMPTZ`, restricciones CHECK de fechas/estados/auditoría y una **restricción de exclusión (EXCLUDE)** que impide en la base dos reservas APROBADAS solapadas en la misma aula.

## API REST

Ruta | Método | Descripción
---|---|---
`/api/aulas` | GET | Catálogo de aulas
`/api/aulas/disponibles` | GET | Aulas disponibles en un rango (solo con reservas APROBADAS)
`/api/aulas/{idAula}` | GET | Detalle de un aula
`/api/solicitudes` | GET / POST | Consultar o registrar una solicitud de reserva (DOCENTE)
`/api/solicitudes/{idSolicitud}/revision` | POST | Revisar una solicitud (ADMINISTRATIVO/ADMINISTRADOR)
`/api/sincronizaciones` | POST | Lanzar una corrida de sincronización con SIGA

## Puesta en marcha

```bash
# 1. Claves locales (el archivo .env se ignora en Git)
cp .env.example .env            # define POSTGRES_PASSWORD y DB_PASSWORD

# 2. Base de datos
docker compose up -d            # contenedor aulasmart-postgres-test (PostgreSQL 17.4, puerto 5433)

# 3. Backend (Flyway aplica las migraciones V1→V3 al arrancar)
cd aulasmart-backend
./mvnw -B -ntp spring-boot:run  # http://localhost:8080

# 4. (Opcional) Datos de prueba y validaciones, en base nueva
docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/seeds/001_datos_prueba.sql
docker compose exec db psql -U aulasmart -d aulasmart_test -f /database/validation/001_validaciones.sql

# 5. Pruebas automatizadas
./mvnw -B test
```

Estructura de la base: `usuario`, `rol`, `usuario_rol`; catálogos sincronizados de SIGA `edificio`, `tipo_aula`, `dependencia` (con `codigo_siga` única); núcleo de reservas `aula` y `solicitud_aula`; y la entidad de control `sincronizacion_siga` (recurso AULAS únicamente).

## Registro

Jhon Edward Steven Loaiza Díaz · Ingeniería de Sistemas · Unidad Central del Valle del Cauca (UCEVA), Tuluá.

La documentación académica del proyecto se gestiona localmente y se excluye del repositorio (ver `/.gitignore`).
