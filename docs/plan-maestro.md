# Plan maestro de evolución de AulaSmart

Estado vigente (2026-11-10): AulaBot publicado en main `1e7da77`. Corrección de solicitudes
aceptada en `feature/solicitudes-historial`, publicación en main autorizada. Las secciones anteriores de fase 5
describen su evolución; prevalece la actualización final de este documento.


Revisión: 2026-11-09. Base publicada: `f432436` (SIGA). Fase 4: `6965a7a`, cierre e integración `develop` → `main` aprobados.

## Orden y alcance

1. **Fase 0: auditoría y estabilización.** Autorizada completa, implementada y
   validada y publicada en `origin/refactor/architecture` (`7030f16`). Se corrigieron hallazgos de
   disponibilidad, concurrencia y seguridad inmediata, conservando el monolito
   modular. Entrega y límites en [fase 0](fase-0.md).
2. **Fase 2: rol único.** Implementada, validada y publicada (`621e3d6`, 2026-11-09). Se simplificaron persistencia,
   DTOs, formularios y permisos; migración explícita para cuentas con cero o varios
   roles, sin eliminar usuarios ni asignar privilegios automáticamente.
   Alcance aprobado en [fase 2](fase-2.md).
3. **Fase 1: consolidar interacción y solicitudes.** Autorizada el 2026-11-09; implementada, validada y publicada (`7c5779f`).
   Entrega y pruebas en [fase 1](fase-1.md). Reutilizar detalles, horarios, formulario, registro y seguimiento existentes.
   Depende de permisos definitivos y disponibilidad fiable.
4. **Fase 3: ajuste de tarjetas.** Alcance acotado por el usuario: solo tarjetas,
   manteniendo colores. Tarjetas aprobadas/publicadas (`f481bad`). Continuación
   de búsquedas publicada (`5fea5b4`). Parte visual aceptada por el usuario.
   Corrección adicional [SIGA](sincronizacion-siga.md) publicada: `f432436`.
5. **Fase 4: configuración definitiva y JWT.** Implementada (`6965a7a`), validada
   y aceptada el 2026-11-09. El usuario autorizó su publicación vía `develop` → `main`
   y dio la fase por terminada. Incluye entornos, configuración HTTPS, sesiones,
   access/refresh, rotación, revocación y CSRF. Entrega y límites en [fase 4](fase-4.md).
   HTTPS instalado requiere certificados propios; no se afirma despliegue real.
6. **Fase 5: AulaBot.** Implementación autorizada el 2026-11-09.
   Groq GPT-OSS 120B, cuenta Free sin facturación confirmada por el usuario.
   Trabajo local en `feature/aulabot`; publicación pendiente de aprobación.
   Entrega, pruebas y límites en [fase 5](fase-5.md).
7. **Fase 6: identidad visual y modo oscuro.** Implementada en `feature/visual-identity-dark-mode`,
   base `main` (`84f8c89`). Sistema de diseño con 60+ tokens CSS, ThemeService reactivo,
   toggle claro/oscuro en header, anti-parpadeo en index.html, mejoras en todos los componentes.
   Build correcto, 100 pruebas frontend aprobadas. Pendiente prueba funcional del usuario
   y autorización de publicación. Entrega en [fase 6](fase-6.md).

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

## Corrección de solicitudes (2026-11-10)

AulaBot publicado en `origin/main`: `1e7da77` (incluye `7db7cde`).
Corrección autorizada en `feature/solicitudes-historial`, base `1e7da77`; usuario aprobó cierre y publicación en main el 2026-11-10.
- Bandeja separada de activas e historial: últimas 50 históricas por ámbito de permisos,
  ordenadas por creación e ID descendente. Docentes solo ven las propias.
- Se conservan hasta 50 históricas por usuario en almacenamiento; las activas no se podan.
  Archivo lógico en la misma tabla al finalizar; los intervalos [inicio, fin) no bloquean
  reservas futuras. No se borra la constancia histórica para liberar el aula.
- Pendientes al llegar inicio: RECHAZADA, sin motivo adicional ni revisor humano ficticio.
  Limpieza automática cada segundo; lectura y revisión también aplican el límite temporal.
  Al reiniciar se procesan vencimientos acumulados. Interfaz se actualiza cada 15 segundos.
- V11 cambia solo la restricción de revisión para permitir rechazo automático sin persona;
  no migra datos ni modifica SIGA. No se borraron datos de la base instalada ni mocks globales.
- Usuario confirmó «quedó bien» y autorizó publicar la corrección el 2026-11-10.
