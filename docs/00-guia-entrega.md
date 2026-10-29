# Guía de los cinco entregables AulaSmart

## Entregables

1. **Diagrama entidad-relación:** [01-modelo-datos.md](01-modelo-datos.md).
2. **Base de datos montada y probada:** [02-base-datos.md](02-base-datos.md).
3. **Diagrama de componentes:** [03-diagrama-componentes.md](03-diagrama-componentes.md).
4. **Diagrama general de arquitectura:** [04-arquitectura-general.md](04-arquitectura-general.md).
5. **Análisis arquitectónico:** [05-analisis-arquitectonico.md](05-analisis-arquitectonico.md).

Los diagramas usan bloques Mermaid dentro de cada documento para mantener una única fuente editable por entregable.

## Evidencias y control

- [Verificación de respuestas SIGA](evidencias/01-fuentes-siga.md).
- [Resultados reales de PostgreSQL](evidencias/02-pruebas-postgresql.md).
- [Estado verificado del backend inicial](07-backend-inicial.md).
- [Registro de puntos de control](06-registro-verificacion.md).

## Verificación rápida de la base existente

Con Docker Desktop en ejecución, desde la raíz:

```powershell
docker compose config --quiet
docker inspect --format '{{.State.Health.Status}}' aulasmart-postgres-test
docker compose exec db psql -U aulasmart -d aulasmart_test -v ON_ERROR_STOP=1 -f /database/validation/001_validaciones.sql
```

El resultado esperado de la comprobación actual es `healthy` y el mensaje final `TODAS LAS VALIDACIONES FINALIZARON SIN ERRORES`. Para crear la base desde cero y conectar pgAdmin, seguir [02-base-datos.md](02-base-datos.md).

## Límites al cierre de esta entrega

La base de datos está montada y probada. El backend Spring Boot que los diagramas presentaban como diseño previsto ya tiene un **primer avance instalado**: consulta de aulas, disponibilidad, seguridad por roles y migración Flyway, con sus verificaciones en [07-backend-inicial.md](07-backend-inicial.md). Todavía no existen la sincronización programada con SIGA, la creación y revisión de solicitudes por API, el frontend Angular ni AulaBot. Tampoco se ejecutó una conexión gráfica desde pgAdmin, aunque la conexión PostgreSQL mediante `psql` sí quedó comprobada.
