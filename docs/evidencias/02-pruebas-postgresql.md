# Evidencia de ejecución PostgreSQL

Fecha: 29 de octubre de 2026.

## Entorno comprobado

- Docker Engine: `28.0.4`.
- Imagen ejecutada: `postgres:17.4-alpine`.
- Base: `aulasmart_test`.
- Usuario: `aulasmart`.
- Conexión TCP verificada: `localhost:5433`.
- Versión devuelta por el servidor: `PostgreSQL 17.4 on x86_64-pc-linux-musl`.

## Comandos ejecutados

Se aplicó `V1__initial_schema.sql`, después `001_datos_prueba.sql` y finalmente `001_validaciones.sql`, todos mediante `psql -v ON_ERROR_STOP=1` dentro del contenedor. También se ejecutó una consulta TCP desde el host que devolvió la base, el usuario y la versión anteriores.

## Resultado de migración y semilla

- Migración: `BEGIN`, extensión `btree_gist`, diez tablas, índices, comentarios y `COMMIT` sin errores.
- Semilla: inserciones de 4 roles, 2 usuarios, 2 asignaciones de rol, 2 edificios, 2 tipos, 1 dependencia, 3 aulas, 1 horario, 3 solicitudes y 2 registros de sincronización; `COMMIT` sin errores.

## Resultado de validaciones

| Prueba | Resultado real |
|---|---|
| Conteos | 3 aulas, 1 horario, 3 solicitudes, 2 usuarios y 4 roles |
| Filtro edificio/capacidad | 1 fila: aula `9001`, `PRUEBA Aula 101`, capacidad 40 |
| Disponibilidad 2026-11-10 08:00–10:00 UTC-5 | 1 fila: aula `9002`, `PRUEBA Aula 102` |
| Conflicto académico | Se encontró el horario `9001` para el aula `9001` |
| Referencias huérfanas | `OK: integridad referencial verificada` |
| Solicitudes aprobadas solapadas | `OK: solapamiento aprobado rechazado` |
| Código externo duplicado | `OK: código externo duplicado rechazado` |
| Intervalo inválido | `OK: intervalo inválido rechazado` |
| Aprobación sin auditoría | `OK: aprobación sin auditoría rechazada` |

Mensaje final recibido: `TODAS LAS VALIDACIONES FINALIZARON SIN ERRORES`.

No se ejecutaron pruebas de aplicación Java, conexión desde Spring Boot ni migración mediante Flyway, porque esos componentes todavía no existen en el repositorio.
