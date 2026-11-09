# Corrección de sincronización SIGA

Fecha: 2026-11-09. Rama: `refactor/classroom-search-ui`.
Publicado: `f432436` en `origin/refactor/classroom-search-ui`, sobre `5fea5b4`.
Publicación autorizada y realizada el 2026-11-09.
Autorización: el usuario reportó errores intermitentes y pidió corregirlos.

## Hallazgos comprobados

- El mensaje del dashboard atribuía cualquier 503 a datos desactualizados. También
  puede haber una corrida en curso, un fallo o cobertura insuficiente.
- El dashboard consultaba solo al entrar: un 503 transitorio permanecía visible
  aunque terminara correctamente la sincronización posterior.
- El arranque comprobaba éxitos históricos, no la cobertura/coherencia vigente.
  Podía actualizar AULAS y omitir EVENTOS por existir eventos recientes, aunque
  estos fueran anteriores al nuevo catálogo y rechazados por disponibilidad.
- La tarea programada ignoraba resultados FALLIDA devueltos sin excepción.
- El bloqueo era individual por recurso: lotes manuales y automáticos podían
  intercalarse entre AULAS y EVENTOS dentro de la misma instancia.

No se inspeccionó la base instalada ni se consultó SIGA vivo. No se ha demostrado
que el intervalo de 15 minutos cause los fallos externos concretos del usuario.

## Correcciones

- Arranque usa la cobertura completa de la ventana configurada en hora Colombia;
  si renueva AULAS, incluye EVENTOS después, aunque hubiera eventos recientes.
- La vigencia del catálogo exige que la última corrida sea exitosa y reciente;
  no utiliza un éxito histórico cuando la última falló o sigue en curso.
- Lotes sincronizados completos comparten el bloqueo de instancia. Arranque y
  tarea programada usan ese lote; no se introducen garantías entre varias réplicas.
- Logs distinguen resultados fallidos de completados. Tras fallo sigue disponible
  el próximo ciclo automático. No se declara éxito porque no hubo excepción.
- Cron conserva 15 minutos, zona Colombia y vigencia de 24 horas. Se permite
  configurarlo mediante AULASMART_SIGA_SYNC_CRON (por defecto: 0 */15 * * * *).
- El dashboard vuelve a consultar disponibilidad local cada 30 segundos, solo
  mientras está montado, sin peticiones superpuestas. Cancela al abandonar la vista.
  Recupera el dato al volver el servicio y elimina valores anteriores ante errores.
  Esta consulta no lanza sincronizaciones ni peticiones a SIGA por cada usuario.
- 503 muestra disponibilidad no verificable sin atribuir una causa no comprobada.

Se preserva la decisión de no afirmar disponibilidad durante una corrida incompleta,
fallida o fuera de cobertura. No se relajaron las validaciones ni se amplió la vigencia.

## Validación

- Backend: mvnw.cmd -o verify con PostgreSQL 17 descartable: 97 pruebas, cero
  fallos, errores u omisiones, paquete generado. No se modificó la base del usuario.
- Frontend: 84 pruebas aprobadas en 21 archivos, incluyendo recuperación 503,
  eliminación de resultados antiguos, no solapamiento y cancelación al destruir.
- git diff --check correcto. Compilación frontend correcta; advertencia CSS preexistente de 10,77 kB / 6 kB.
- Base descartable detenida al finalizar las pruebas.

## Verificación instalada pendiente

Reiniciar el backend con la corrección y abrir el dashboard. Si hay una corrida en
curso, esperar a su finalización: el panel debe recuperarse sin recargar. Si el
fallo persiste, revisar estado, tiempos y detalle de la última corrida de AULAS y
EVENTOS; ese registro permite distinguir timeout/red, respuesta inválida y cobertura.
No aumentar intervalos de vigencia para ocultar fallos. No se requiere migración.
