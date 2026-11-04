# AulaSmart

AulaSmart es un sistema web académico de **reservas de aulas** para la Unidad Central del Valle del Cauca (UCEVA): los docentes registran solicitudes de reserva con semántica de intervalo medio abierto `[inicio, fin)`, el personal administrativo las revisa (aprobar/rechazar), el catálogo de aulas y la ocupación real por clases se mantienen sincronizados desde el sistema SIGA de la universidad, y el administrador gestiona las cuentas de todos los usuarios.

> Estado actual: backend monolito modular **implementado y verificado** (pruebas Maven en verde y validaciones SQL automatizadas contra la base) y SPA Angular **implementada** (dashboard, consulta de disponibilidad, solicitudes, gestión de aulas, usuarios y sesión).

## Arquitectura

Backend único tipo **monolito modular** con Spring Boot 3.x (Java 25) organizado con Clean Architecture (`domain · application · infrastructure · presentation`), una única base PostgreSQL y una sola canalización de despliegue. La SPA Angular replica la misma separación por feature (`domain · application · infrastructure · presentation`).

| Módulo | Responsabilidad | Estado |
|---|---|---|
| `users` | Cuentas, roles y seguridad; directorio y creación de cuentas exclusivos del ADMINISTRADOR | Verificado |
| `aulas` | API REST de catálogo y reglas de disponibilidad (puerta de calidad de sincronización ≤ 24 h) | Verificado |
| `solicitudes` | Registro, revisión y consulta de reservas con auditoría y re-verificación de solape | Verificado |
| `integrations.siga` | Sincronización del catálogo de aulas (AULAS) y de la ocupación académica por clases (EVENTOS), manual y programada, con transformación estricta y registro de corridas | Verificado |
| `persistencia` | Repositorios Spring Data JPA y migraciones Flyway compartidos | Verificado |
| Frontend Angular | SPA de consulta, solicitudes, gestión de aulas, usuarios y autenticación | Implementado |
| `chatbot` + `integrations.ai` | AulaBot con proveedor de IA (API Gemini); puerto definido, adaptador pendiente | Planeado |

La decisión monolito modular frente a microservicios se revisará solo con evidencia de equipos autónomos, cargas de escalado distintas o requisitos de aislamiento; los puertos internos permiten extraer un componente sin reescribir el resto.

## Stack y roles

- **Java 25 / Spring Boot 3.5.7**: Web MVC, Security (autenticación Basic y autorización por rol), Validation, Data JPA (Hibernate), Actuator.
- **Angular 20** (SPA, señales y standalone components; `npm start` en `http://localhost:4200`).
- **PostgreSQL 17.4** en Docker Compose (puerto 5433, base `aulasmart_test`), migraciones Flyway V1→V4.
- **Roles**: `ESTUDIANTE`, `DOCENTE`, `ADMINISTRATIVO`, `ADMINISTRADOR`. El docente crea solicitudes; el administrativo revisa; **solo el administrador crea cuentas** (el administrativo no ve la gestión de usuarios).

La integridad de las reservas no depende de la aplicación sola: intervalos `TIMESTAMPTZ`, restricciones CHECK de fechas/estados/auditoría y una **restricción de exclusión (EXCLUDE)** que impide en la base dos reservas APROBADAS solapadas en la misma aula.

## API REST

Ruta | Método | Descripción
---|---|---
`/api/aulas` | GET | Catálogo de aulas
`/api/aulas/disponibles` | GET | Aulas libres en una franja: descuenta reservas APROBADAS y clases del calendario académico de SIGA (`horario_academico`); exige sincronización vigente de AULAS y EVENTOS
`/api/aulas/{idAula}` | GET | Detalle de un aula
`/api/solicitudes` | GET / POST | Consultar o registrar una solicitud de reserva (DOCENTE)
`/api/solicitudes/{idSolicitud}/revision` | POST | Revisar una solicitud (ADMINISTRATIVO/ADMINISTRADOR)
`/api/usuarios` | GET / POST | Directorio y creación de cuentas (solo ADMINISTRADOR)
`/api/usuarios/whoami` | GET | Perfil de la sesión activa (correo, nombre real y roles)
`/api/sincronizaciones` | POST | Lanzar una corrida de sincronización con SIGA (`recurso=AULAS` o `recurso=EVENTOS`)

## Puesta en marcha

```bash
# 1. Claves locales (el archivo .env se ignora en Git)
cp .env.example .env            # define POSTGRES_PASSWORD y DB_PASSWORD

# 2. Base de datos
docker compose up -d            # contenedor aulasmart-postgres-test (PostgreSQL 17.4, puerto 5433)

# 3. Backend (Flyway aplica las migraciones V1→V4 al arrancar)
cd aulasmart-backend
./mvnw -B -ntp spring-boot:run  # http://localhost:8080

# 4. SPA
cd ../aulasmart-frontend
npm install && npm start        # http://localhost:4200

# 5. (Opcional) Datos de prueba y validaciones, en base nueva
docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/seeds/001_datos_prueba.sql
docker compose exec db psql -U aulasmart -d aulasmart_test -f /database/validation/001_validaciones.sql

# 6. Pruebas automatizadas
cd aulasmart-backend && ./mvnw -B test
cd ../aulasmart-frontend && npm test -- --watch=false
```

Cuentas sintéticas de prueba (viven en la semilla, credenciales no institucionales): `docente@prueba.aulasmart.local` (DOCENTE), `revisor@prueba.aulasmart.local` (ADMINISTRATIVO) y `admin@prueba.aulasmart.local` (ADMINISTRADOR); todas con clave `password`. La sincronización de eventos descarga la ocupación por clases del calendario público de SIGA (`publicomanejoespacios`) para la ventana configurable `aulasmart.siga.eventos.dias-ventana` (7 días por omisión).

Estructura de la base: `usuario`, `rol`, `usuario_rol`; catálogos sincronizados de SIGA `edificio`, `tipo_aula`, `dependencia` (con `codigo_siga` única); núcleo de reservas `aula` y `solicitud_aula`; ocupación académica `horario_academico` (fuente SIGA); y la entidad de control `sincronizacion_siga` (recursos AULAS y EVENTOS).

## Registro

Jhon Edward Steven Loaiza Díaz · Ingeniería de Sistemas · Unidad Central del Valle del Cauca (UCEVA), Tuluá.

La documentación académica del proyecto se gestiona localmente y se excluye del repositorio (ver `/.gitignore`).
