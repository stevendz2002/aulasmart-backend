# Registro de puntos de control

Fecha de cierre: 29 de octubre de 2026.

| Punto de control | Estado | Trabajo y evidencia | Commits |
|---|---|---|---|
| 0. Inspección inicial | COMPLETADO | Repositorio y Git inspeccionados; aulas SIGA verificadas; eventos respondieron vacíos; diagnóstico en `docs/evidencias/01-fuentes-siga.md` | `15e3716` |
| 1. Modelo de datos | COMPLETADO | Un diagrama ER, diez tablas, claves, restricciones, ocupaciones separadas de solicitudes y control de sincronización | `15e3716`, `7836254` |
| 2. Base de datos | COMPLETADO | PostgreSQL 17.4 saludable; esquema y semilla aplicados; conexión TCP y validaciones ejecutadas | `15e3716` |
| 3. Componentes | COMPLETADO | Diagrama coherente con Angular, módulos Spring Boot, persistencia y adaptadores externos | `8458903` |
| 4. Arquitectura general | COMPLETADO | Actores, canales, unidades desplegables, PostgreSQL, SIGA y proveedor IA diferenciados | `8458903` |
| 5. Decisión arquitectónica | COMPLETADO | Comparación contextual y decisión justificada de monolito modular con Clean Architecture | `6e91226` |
| 6. Git y calidad | COMPLETADO | Rama `feat/entregables-academicos`, cuatro commits, revisión de diffs, `.env` excluido e integración local en `main` | Integración `85f49f2` |
| 7. Revisión final | COMPLETADO | Revisión cruzada sin bloqueos; se corrigió la representación de unicidad externa y se repitieron las pruebas SQL | `7836254` y registro final |

## Verificaciones ejecutadas

- `git status`, ramas, historial y diferencias antes de modificar e integrar.
- `git diff --check`: sin errores pendientes al cierre.
- `docker compose config --quiet`: configuración válida.
- Contenedor `aulasmart-postgres-test`: estado `healthy`.
- Migración `V1__initial_schema.sql`: finalizó con `COMMIT` y sin errores.
- Semilla sintética: finalizó con `COMMIT` y sin errores.
- `001_validaciones.sql`: ejecutado dentro del contenedor y nuevamente desde el host mediante TCP; ambas ejecuciones finalizaron sin errores.
- Revisión de coherencia de los cinco documentos y el esquema: sin bloqueos; una inconsistencia menor del diagrama fue corregida.

## Decisiones principales

- Identificadores internos independientes y códigos SIGA únicos para catálogos y aulas.
- `TIMESTAMPTZ` e intervalos `[inicio, fin)` para ocupaciones.
- Restricción de exclusión para solicitudes aprobadas solapadas y validación transaccional adicional contra eventos en el futuro backend.
- Historial de AulaBot no persistente en esta etapa por no ser necesario para el alcance.
- Monolito modular único, Clean Architecture interna y patrones Adapter/Repository justificados; sin microservicios ni infraestructura distribuida.

## Pendientes o limitaciones

- No se obtuvieron objetos de eventos SIGA; campos internos e identificador externo siguen sin confirmar.
- Frecuencia y umbral de vigencia de sincronización pendientes.
- Disponibilidad y condiciones de Gemini pendientes.
- Conexión gráfica de pgAdmin documentada, pero no ejecutada; `psql` sí se comprobó.
- No existen todavía backend, frontend ni pruebas Java; no se afirma compilación ni despliegue de aplicación.
- No se hizo `push`; los commits están únicamente en el repositorio local.
