# Plan maestro de evolución de AulaSmart

Revisión: 2026-11-08. Base: `7030f16`; fase 2 local en `feature/single-user-role`.

## Orden y alcance

1. **Fase 0: auditoría y estabilización.** Autorizada completa, implementada y
   validada y publicada en `origin/refactor/architecture` (`7030f16`). Se corrigieron hallazgos de
   disponibilidad, concurrencia y seguridad inmediata, conservando el monolito
   modular. Entrega y límites en [fase 0](fase-0.md).
2. **Fase 2: rol único.** Implementada, validada y aprobada para publicación (2026-11-09). Se simplificaron persistencia,
   DTOs, formularios y permisos; migración explícita para cuentas con cero o varios
   roles, sin eliminar usuarios ni asignar privilegios automáticamente.
   Alcance aprobado en [fase 2](fase-2.md).
3. **Fase 1: consolidar interacción y solicitudes.** Autorizada el 2026-11-09.
   Reutilizar detalles, horarios, formulario, registro y seguimiento existentes.
   Depende de permisos definitivos y disponibilidad fiable.
4. **Fase 3: rediseño de consulta.** Pendiente de autorización. Propuesta visual
   antes de implementación; accesibilidad, responsive y componentes reutilizados.
5. **Fase 4: configuración definitiva y JWT.** Pendiente de autorización.
   Completar entornos, HTTPS, sesiones, access/refresh tokens, rotación, revocación
   y evaluación de cookies/CSRF. No guardar tokens de usuario en `.env`.

Se adelanta el rol único para evitar rehacer los permisos de solicitudes y JWT.
La configuración por entorno y BCrypt ya existen; no deben reconstruirse.
Estimación inicial orientativa: fase 0, 3–5 jornadas; fase 2, 3–5; fase 1, 3–5;
fase 3, 3–5; fase 4, 5–8. Reestimar al aprobar cada alcance.

## Gestión eficiente del contexto

- Construir una visión global durante la auditoría inicial y conservarla en `/docs`.
- Mantener `AGENTS.md` en la raíz y usarlo con estos documentos en siguientes sesiones.
- Al iniciar cada fase: consultar documentación y Git; verificar su vigencia.
- Revisar solo módulos, archivos y dependencias relacionados con la tarea.
- No volver a explorar todo el repositorio sin cambios arquitectónicos significativos
  o información insuficiente.
- Después de cada fase aprobada, actualizar decisiones, cambios, pruebas y pendientes.
- No asumir que la documentación es actual: contrastarla con el código cuando corresponda.
- Evitar archivos generados e irrelevantes (`node_modules`, `dist`, `target`, `.git`).

Objetivo: mantener continuidad entre sesiones y reducir análisis repetitivos sin
sacrificar calidad. La autorización de toda la fase 0 no autoriza fases posteriores
ni commits. La siguiente fase requiere su checkpoint de aprobación.
