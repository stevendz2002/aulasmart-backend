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
  Posteriormente aprobó publicar fase 4 en `develop` y después `main`, y cerrar la fase.
  No repetir la auditoría global.

## Punto de continuidad (2026-11-09)

- Publicado: fase 0 `7030f16`, fase 2 `621e3d6`, fase 1 `7c5779f`.
- Fase 3: tarjetas `f481bad` y búsquedas `5fea5b4`, aprobadas por el usuario.
  Sus decisiones visuales están en `docs/fase-3.md`; conservarlas.
- SIGA publicado: `f432436` en `origin/refactor/classroom-search-ui`.
  Consultar `docs/sincronizacion-siga.md`; no se comprobó SIGA vivo.
- Fase 4 implementada en `6965a7a`, origen `feature/jwt-authentication`; aceptada.
  Entrega autorizada a `develop` y después `main`. Consultar Git para la rama activa
  y `docs/fase-4.md` para decisiones y evidencia. No hay otra fase activa.
- Implementado: JWT Bearer, refresh HttpOnly con rotación/revocación en PostgreSQL,
  V10 aditiva, API relativa /api y perfiles local/prod con configuración HTTPS.
  No restaurar Basic. No guardar contraseñas ni tokens individuales de usuarios en almacenamiento web o env.
- Validado: 109 pruebas backend, 91 frontend, compilaciones y configuración Compose.
  PostgreSQL descartable detenido; no se modificó la base instalada del usuario.
- El usuario aprobó explícitamente la publicación y el cierre de fase 4 el 2026-11-09.
- Límites de entorno: HTTPS real requiere certificados propios; SIGA vivo no verificado
  por el agente. Advertencia CSS previa documentada como mantenimiento.
- Fase 4 publicada y cerrada: `5e4b4da` en `develop` y `main`.
- Fase 5: usuario autorizó implementar con Groq GPT-OSS 120B y continuar el trabajo
  de Kimi. Cuenta Free sin facturación confirmada. Rama `feature/aulabot`, base `5e4b4da`.
- Consultar `docs/fase-5.md`. Cambios locales sin commits; publicación pendiente.
- Costo API cero: no activar facturación ni fallback de pago. Clave solo en `.env`
  ignorado; no imprimirla ni copiarla a código, docs, reportes o frontend.
- AulaBot interpreta filtros, pero disponibilidad y acciones provienen del backend.

- Fase 5 local validada: 125 pruebas backend, prueba Groq separada con tres llamadas
  sintéticas, 99 pruebas frontend; ambas compilaciones y Compose correctos.
  PostgreSQL descartable detenido. Pendiente prueba del usuario; no publicar todavía.

- Revisión 5A–5E de la fase 5 autorizada e implementada el 2026-11-10 en el mismo
  árbol local de `feature/aulabot`, sin commits. Memoria conversacional en servidor
  (sin Redis, TTL 30 min, deduplicación 90 s), equipamiento resuelto contra nombres
  reales del catálogo (sin migración), alternativas reales ante búsquedas sin
  resultados (chips con conteos verificados localmente) y un solo chat (eliminado
  el endpoint y el modo «Consulta guiada»). El cliente ya no envía contexto; solo
  `{mensaje, nuevaConversacion}`. Ver `docs/fase-5.md`, sección «Revisión autorizada 5A–5E».
  Validación: `mvnw.cmd -o verify` con PostgreSQL descartable, 139 pruebas backend,
  99 frontend, builds y Compose correctos; base descartable detenida.
  GroqConexionTest actualizada al flujo `SolicitudInterpretacion` (opt-in, sin reejecutar).
  Pendiente: prueba funcional del usuario, reemplazar la clave Groq expuesta en el
  chat y autorización de publicación. Sin otra sub-fase activa.
- Corrección de fase 5 tras la prueba del usuario (2026-11-10), sin commits en el
  mismo árbol: la aclaración ya conserva el filtro parcial entendido (mezcla con lo
  vigente y se muestra en `filtros`), la deduplicación se firma con el estado
  (repetir un mensaje con otro contexto se interpreta de nuevo; el registro queda
  contra el estado resultante del turno) y NO_VERIFICABLE ofrece el chip «Busca en
  el catálogo sin horario» (el frontend lo muestra igual que en SIN_RESULTADOS).
  Validado: 143 pruebas backend tras recrear la base descartable limpia, 99 frontend,
  build correcto y conversación reproducida en vivo contra PostgreSQL descartable
  (filtros conservados en los tres turnos y CATALOGO que lleva a Solicitar).
  Pendiente: prueba funcional del usuario con esta corrección, nueva clave Groq
  y autorización de publicación.
- Rechazo de la gestión de solicitudes desde el chat (respuesta a la prueba del
  usuario): chat informativo pregunta a pregunta sin memoria multi-turno, contrato
  sin `puedeSolicitar`/`acciones`, aclaraciones con ejemplo de frase completa.
  Validado: 137 backend, 98 frontend, builds correctos y conversación de prueba
  reproducida en vivo. Sin otra sub-fase activa.
- Cambio a IA totalmente determinista y a un lado de Groq (respuesta a la petición
  del usuario): `InterpretacionDeterminista` local reemplaza a `GroqInterpretador`
  como bean primario del puerto; la interpretación sale de reglas locales sobre el
  texto. Validado: 137 backend, repro en vivo con respuestas deterministas y sin
  consumo de cuota Groq; frontend sin cambios (98 pruebas, build OK).
  Sin otra sub-fase activa.
- Arquitectura híbrida final (2026-11-10), tras nueva petición del usuario: Groq
  solo extrae JSON; motor determinista con consulta vigente + franja pendiente
  (fecha y horario pueden llegar en turnos distintos), plantillas backend, fechas
  relativas resueltas en backend y chips como acciones estructuradas sin Groq.
  Cuota: 6/min/usuario y 20/min global, acciones sin cuota, fallo → respuesta guiada
  sin 503. Validado: 146 backend, 99 frontend, build OK. Se eliminó el interpretador
  local determinista intermedio. Rama `feature/aulabot`; autorizado commit/push.
  Publicado: `7db7cde` en `origin/feature/aulabot`.
