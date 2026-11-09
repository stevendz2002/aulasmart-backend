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
  No restaurar Basic. No guardar contraseñas ni tokens en almacenamiento web o env.
- Validado: 109 pruebas backend, 91 frontend, compilaciones y configuración Compose.
  PostgreSQL descartable detenido; no se modificó la base instalada del usuario.
- El usuario aprobó explícitamente la publicación y el cierre de fase 4 el 2026-11-09.
- Límites de entorno: HTTPS real requiere certificados propios; SIGA vivo no verificado
  por el agente. Advertencia CSS previa documentada como mantenimiento.
