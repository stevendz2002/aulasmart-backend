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
- [Estado verificado de la sincronización con SIGA](08-sincronizacion-siga.md).
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

La base de datos está montada y probada. El backend ha incorporado un **segundo avance verificado**: además de la consulta de aulas, disponibilidad, seguridad por roles y migración Flyway del [backend inicial](07-backend-inicial.md), ahora existe la **sincronización con SIGA** —manual y programada, con filas de control— comprobada con 104 aulas reales en la base de pruebas y detallada en [08-sincronizacion-siga.md](08-sincronizacion-siga.md). Todavía no existen la creación y revisión de solicitudes por API, el frontend Angular ni AulaBot, y la confirmación de la estructura de eventos propios de SIGA sigue pendiente. Tampoco se ejecutó una conexión gráfica desde pgAdmin, aunque la conexión PostgreSQL mediante `psql` sí quedó comprobada.
