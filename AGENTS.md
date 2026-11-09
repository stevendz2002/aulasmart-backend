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
- El 2026-11-09 el usuario aprobó publicar la fase 2 (`feature/single-user-role`)
  y continuar fase 1 en `feature/classroom-request`; informó migración sin conflictos.
- El usuario autorizó publicar fase 1 y comenzar fase 3 el 2026-11-09.
  El usuario acotó después el cambio a las tarjetas de resultados, conservando colores.
- El 2026-11-09 el usuario autorizó publicar la corrección SIGA y continuar fase 4.
  Los cambios nuevos de fase 4 deben quedar locales hasta su prueba y aprobación. No repetir la auditoría global.

## Punto de continuidad (2026-11-09)

- Fase 2 publicada: `621e3d6` en `origin/feature/single-user-role`.
- Fase 1 publicada: `7c5779f` en `origin/feature/classroom-request`.
- Rama activa: `refactor/classroom-search-ui`; propuesta de fase 3 en `docs/fase-3.md`.
- Alcance vigente de fase 3: solo distribución y legibilidad de tarjetas de aulas;
  conservar identidad azul/blanca, filtros, diálogos y comportamiento. El usuario
  aprobó jerarquía de nombre/código/capacidad, iconos uniformes, Libre verde y Ocupada rojo
  según la última referencia, sin borde lateral, botones como la referencia: Solicitar principal en libres, Ver aula principal
  en ocupadas/sin verificar; fondos pastel en iconos y etiquetas; cinco columnas
  en escritorio y adaptación móvil. El usuario aclaró que quiere iconos, no fotografías:
  usar icono azul sobre recuadro suave según tipo de aula, como su referencia.
  Evitar textos íntegramente en mayúsculas en tarjetas; preservar códigos y siglas. Conservar estado textual. La propuesta
  amplia anterior no está aprobada. Consultar `docs/fase-3.md`.

- El usuario aprobó publicar las tarjetas actuales y continuar con búsquedas:
  conectar resultados de búsqueda principal con consulta/detalle/solicitud y
  alargar horizontalmente el buscador de consulta sin aumentar su altura
  (aclaración explícita del usuario). Tarjetas publicadas: `f481bad`.
- Búsquedas aprobadas y publicadas: `5fea5b4`. Parte visual aprobada por el usuario.
- Trabajo actual autorizado: corregir sincronización SIGA; cambios locales en la
  misma rama. Consultar `docs/sincronizacion-siga.md` antes de continuar.
- Próximo trabajo autorizado: fase 4, configuración y JWT, en rama independiente.
  No publicar sus cambios hasta que el usuario los pruebe y lo autorice.
