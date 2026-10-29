# Entregable 2: base de datos PostgreSQL

## Estado verificado

La base **`aulasmart_test`** se montó en un contenedor Docker con **PostgreSQL 17.4**. El esquema, los datos sintéticos y las validaciones se ejecutaron correctamente el 29 de octubre de 2026. La conexión TCP también se comprobó desde el equipo mediante `psql`, usando el puerto publicado `5433`.

El contenedor se llama `aulasmart-postgres-test`. La actividad no instaló pgAdmin ni comprobó una conexión gráfica; las instrucciones de conexión se incluyen más abajo.

## Archivos

- `compose.yaml`: servicio PostgreSQL, volumen persistente, puerto y chequeo de salud.
- `.env.example`: plantilla sin credenciales reales.
- `.env`: configuración local ignorada por Git.
- `database/migrations/V1__initial_schema.sql`: esquema con claves y restricciones.
- `database/seeds/001_datos_prueba.sql`: datos marcados con `PRUEBA`; no son datos institucionales.
- `database/validation/001_validaciones.sql`: consultas y aserciones ejecutables.

El nombre de la migración es compatible con Flyway, pero **Flyway no se ejecutó** porque el backend Maven todavía no existe. En esta entrega el archivo se aplicó directamente con `psql`.

## Reproducción

Requisitos: Docker Desktop con Docker Compose. No se requiere Maven ni una instalación local de PostgreSQL para usar el contenedor.

1. Copiar `.env.example` como `.env` y establecer una clave local propia.
2. Desde la raíz del repositorio, iniciar el servicio:

   ```powershell
   docker compose up -d
   ```

3. Esperar hasta que el estado sea saludable:

   ```powershell
   docker inspect --format '{{.State.Health.Status}}' aulasmart-postgres-test
   ```

4. En una base nueva, aplicar esquema y datos de prueba **una sola vez**:

   ```powershell
   docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/migrations/V1__initial_schema.sql
   docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/seeds/001_datos_prueba.sql
   ```

5. Ejecutar las comprobaciones:

   ```powershell
   docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/validation/001_validaciones.sql
   ```

La semilla no es idempotente y no debe ejecutarse nuevamente sobre los mismos datos. Está pensada para una base de pruebas nueva, no para producción.

## Conexión desde pgAdmin

En **Register > Server**, conservar un nombre descriptivo en la pestaña General y usar:

| Parámetro | Valor |
|---|---|
| Host name/address | `localhost` |
| Port | `5433` (o el valor de `POSTGRES_PORT`) |
| Maintenance database | `aulasmart_test` |
| Username | `aulasmart` |
| Password | el valor del `.env` local |

Al expandir `Databases > aulasmart_test > Schemas > public > Tables` deben aparecer diez tablas: `usuario`, `rol`, `usuario_rol`, `edificio`, `tipo_aula`, `dependencia`, `aula`, `horario_academico`, `solicitud_aula` y `sincronizacion_siga`.

## Pruebas realizadas

1. Conteo de entidades: 3 aulas, 1 horario, 3 solicitudes, 2 usuarios y 4 roles sintéticos.
2. Filtro por edificio, capacidad mínima 40 y aula activa: devolvió únicamente `PRUEBA Aula 101`.
3. Disponibilidad para el 10 de noviembre de 2026, 08:00–10:00 (UTC-5), capacidad mínima 30: devolvió únicamente `PRUEBA Aula 102`; el horario sintético excluyó el aula 9001.
4. Integridad referencial: sin referencias huérfanas entre las relaciones comprobadas.
5. Solicitud aprobada solapada: PostgreSQL la rechazó mediante la restricción de exclusión.
6. Código SIGA duplicado: PostgreSQL lo rechazó mediante la restricción única.
7. Intervalo con fin anterior al inicio: rechazado por restricción `CHECK`.
8. Aprobación sin revisor ni fecha: rechazada por restricción `CHECK`.

La salida termina con `TODAS LAS VALIDACIONES FINALIZARON SIN ERRORES`. Estas pruebas verifican el modelo relacional, no un backend Spring Boot que aún no existe.

## Nota operativa

Durante el arranque Docker informó la existencia de otros contenedores huérfanos del mismo nombre de proyecto. No se eliminaron ni modificaron. No usar `--remove-orphans` en este entorno compartido.
