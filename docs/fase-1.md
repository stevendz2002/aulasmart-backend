# Fase 1: interacción con aulas y solicitudes

Revisión: 2026-11-09. Rama: `feature/classroom-request`, base `621e3d6`.
Estado: implementada, validada y publicada en `origin/feature/classroom-request`,
commit `7c5779f` (2026-11-09).

## Análisis y alcance

Se consultaron primero AGENTS, índice, plan maestro y arquitectura. La revisión
se limitó a consulta de aulas, bandeja, modelos, casos de uso, controlador y pruebas
relacionadas. No se repitió una auditoría del repositorio.

Ya existían detalle (bloque, capacidad, características), horarios de la fecha,
selección de franjas, registro y seguimiento. El backend verifica aula activa,
solapes locales/académicos y SIGA al registrar; vuelve a verificar al aprobar.
La autorización se aplica en backend. Se preservan esos casos de uso y endpoints.

Hallazgos: acciones de solicitud visibles a estudiantes, confirmación sin ID que
se cerraba automáticamente, dos diálogos superpuestos, envíos/revisiones repetibles,
error de carga acompañado de espera indefinida, horarios de bandeja dependientes
de la zona del navegador y campo Asistentes sin persistencia.

Plan ejecutado dentro de la autorización de la fase: corregir esas interacciones,
conectar confirmación con bandeja, mantener permisos y cubrir regresiones.
No se incorporaron dependencias, migraciones ni escrituras institucionales.

## Cambios y decisiones

- Solicitar solo para docente, administrativo y administrador; estudiante conserva detalles.
- Confirmación persistente con ID y estado real, aclaración de no reserva institucional
  y enlace a seguimiento filtrado. Se elimina el cierre automático de 1,5 segundos.
- Un solo diálogo visible. No cerrar ni repetir envío mientras se procesa; tras éxito
  no reenviar la misma solicitud desde el formulario. Esto no es idempotencia de servidor.
- La bandeja permite registro a los tres roles autorizados, bloquea revisión concurrente
  desde la misma interfaz y habilita reintento si falla. El backend sigue decidiendo permisos.
- Errores de catálogo/listado visibles con reintento, sin indicar carga permanente.
- Horarios enviados e interpretados en Colombia; etiquetas de franjas indican la fecha elegida.
- Se retira Asistentes del formulario: la API no lo recibía ni almacenaba. No se pierde
  información persistida. No se agrega un contrato nuevo fuera del alcance.
- Se conserva el diseño existente; el rediseño visual corresponde a fase 3.

## Criterios de aceptación y pruebas

- Consultar detalle y horario sin confundir falta de datos con disponibilidad.
- Estudiante sin acción de registro; permisos backend existentes preservados.
- Registrar y conservar confirmación con ID/estado, abrir seguimiento.
- Evitar envíos/revisiones repetidos mientras están pendientes; permitir reintento ante error.
- Mantener errores 409/503 como fallo, nunca como confirmación de reserva.
- Suite frontend y compilación de producción; no es prueba manual contra SIGA vivo.

## Pendientes

- Validación automatizada completada el 2026-11-09.
- Publicación autorizada por el usuario; no se afirma una prueba manual adicional.
- Fase 3 autorizada para iniciar con propuesta visual; fase 4 pendiente; la base migrada sin conflictos fue confirmada por el usuario.

## Resultados de validación

- `npm test -- --watch=false`: 74 pruebas aprobadas en 18 archivos.
- `npm run build`: correcto; persiste advertencia previa de CSS de consulta
  (8,42 kB frente a presupuesto de 6 kB).
- `git diff --check`: correcto.
- Backend sin cambios; se conserva la evidencia de 95 pruebas de fase 2,
  sin presentarla como una nueva ejecución de esta fase.
- No se accedió a SIGA vivo ni a la base del usuario para estas pruebas.

## Revisión manual sugerida

1. Como estudiante, abrir detalles y horario: no debe aparecer Solicitar.
2. Como docente, buscar aula, abrir detalle, elegir fecha/franja y enviar motivo.
3. Verificar confirmación persistente con SOL-ID y PENDIENTE, abrir Ver estado.
4. Como administrativo o administrador, revisar y comprobar el cambio de estado.
5. Si hay solape o disponibilidad no verificable, debe aparecer un error sin éxito.

## Revisión adicional del seguimiento

- Búsqueda SOL-ID exacta: el enlace SOL-77 no devuelve SOL-177 ni motivos con 77.
- Se descartan respuestas y errores de listados anteriores cuando existe una consulta
  más reciente, evitando restablecer estados antiguos tras decisiones concurrentes.
- Una decisión confirmada actualiza la lista local antes de recargar; se conserva
  aunque falle la recarga, cuyo error se comunica por separado.
- Cuatro regresiones adicionales: suite final de 74 pruebas aprobadas; build correcto
  con la misma advertencia CSS. Sin cambios de backend, base de datos ni publicación.
