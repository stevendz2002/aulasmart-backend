# AulaSmart: instrucciones para trabajar en el repositorio

## Continuidad y contexto

- Al iniciar una sesión o fase, leer `docs/README.md`, `docs/plan-maestro.md`
  y el estado de la fase correspondiente; comprobar rama, cambios locales y último commit.
- Verificar la vigencia de los documentos con el código pertinente; no tratarlos como prueba de ejecución.
- Consultar `docs/arquitectura.md` para ubicar módulos y dependencias.
- Explorar únicamente los archivos relacionados con la tarea y sus dependencias.
- Repetir una auditoría global solo ante cambios arquitectónicos significativos o información insuficiente.
- Evitar cargar `node_modules`, `dist`, `target`, `.git` y otros archivos generados.
  Consultar un reporte generado concreto únicamente si se necesita verificar una ejecución.
- No leer ni imprimir valores secretos de `.env`; consultar `.env.example` y las referencias de configuración.
- Después de cada fase aprobada, actualizar decisiones, cambios, pruebas reales y pendientes en `/docs`.
- Registrar fecha, rama y referencia Git; indicar cuando el estado descrito incluye cambios sin commit.
- Mantener separados los hechos comprobados, las hipótesis y el trabajo propuesto.

## Alcance y aprobación

- Mejorar la implementación existente; no reescribirla desde cero.
- Seguir análisis → plan → aprobación → implementación → validación → entrega.
- La autorización explícita de una fase completa cubre sus correcciones necesarias;
  no pedir nuevamente aprobación para decisiones rutinarias dentro de ese alcance.
- No avanzar a otra fase sin autorización explícita.
- No crear commits, cambiar ramas ni publicar cambios sin autorización del usuario.
- Preservar cambios ajenos. Usar una rama independiente por fase.
- No declarar una fase terminada si faltan sus criterios de aceptación o validaciones requeridas.

## Arquitectura y calidad

- Backend: Java 17, Spring Boot, PostgreSQL, monolito modular con puertos y adaptadores.
- Frontend: Angular y TypeScript por features; reutilizar componentes y servicios existentes.
- Mantener el dominio independiente de presentación y persistencia; justificar nuevas abstracciones.
- Comprobar permisos en backend, validar entradas y usar el manejo centralizado de errores.
- Una solicitud local o aprobación de AulaSmart no equivale a una reserva institucional confirmada.
- Verificar capacidades reales de SIGA antes de asumir escrituras institucionales.
- Mantener identidad azul y blanca; incluir estados de carga, error y disponibilidad desconocida.
- Añadir pruebas proporcionales al impacto y ejecutar compilación/pruebas pertinentes.
- Las pruebas de migración/concurrencia deben usar PostgreSQL descartable, nunca datos del usuario.

## Estado de autorización

- Fase 0 completa autorizada en esta sesión; rama autorizada: `refactor/architecture`.
- El usuario autorizó registrar y subir la fase 0 y continuar con fase 2 en
  `feature/single-user-role`. Fases 1, 3 y 4 siguen pendientes de autorización.
- Estado técnico y resultados actualizados: `docs/fase-0.md`.
