# Documentación técnica de continuidad

Última revisión: 2026-11-09. Rama: `refactor/classroom-search-ui`.

1. Leer [plan maestro](plan-maestro.md) para alcance y autorizaciones.
2. Consultar [arquitectura](arquitectura.md) para ubicar los módulos relevantes.
3. Leer [fase 0](fase-0.md) para cambios, validaciones y pendientes.
   Consultar la implementación y validación de [fase 2](fase-2.md).
4. Contrastar lo necesario con Git y el código antes de trabajar.

La fase 0 está publicada en `origin/refactor/architecture`, commit `7030f16`.
La fase 2 está publicada en `origin/feature/single-user-role`, commit `621e3d6`.
Fase 1 publicada en `origin/feature/classroom-request`: `7c5779f`.
Tarjetas aprobadas y publicadas: `f481bad` en `origin/refactor/classroom-search-ui`.
Trabajo activo: interacción de búsquedas, documentada en [fase 3](fase-3.md).
No sustituyen la inspección de diferencias ni demuestran que una base instalada
ya recibió las migraciones. El informe de auditoría previo mencionado por el
usuario no fue localizado; esta documentación registra la revisión actual.

Los archivos académicos preexistentes siguen siendo locales e ignorados por Git.
Solo los documentos técnicos indicados explícitamente en `.gitignore` están habilitados para versionarse.
`AGENTS.md` contiene las reglas permanentes de trabajo.

## Actualizaciones

Al cerrar una fase: registrar fecha, referencia Git, alcance implementado,
decisiones, pruebas con resultado real, limitaciones y próximos pasos autorizados.
No repetir listados completos ni copiar reportes generados; enlazar archivos fuente
y resumir evidencia verificable. Si el código contradice un documento, corregir
el documento y dejar constancia de la discrepancia.
