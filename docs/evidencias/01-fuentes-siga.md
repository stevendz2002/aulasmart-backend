# Verificación de fuentes SIGA

Fecha de comprobación: **29 de octubre de 2026**. Las siguientes observaciones proceden de respuestas HTTP reales obtenidas durante esta actividad.

## Aulas

Se consultó:

`https://uceva.datasae.co/siga_new/web/app.php/publicomanejoespacios/listarAulasPublico?page=1&start=0&limit=25`

La respuesta fue un JSON válido con raíz:

- `success`: booleano; valor observado `true`.
- `data`: arreglo de aulas.
- `total`: entero; valor observado `104`.

Los 104 objetos contienen los mismos once campos, sin valores nulos en esta respuesta:

| Campo | Tipo observado | Uso propuesto |
|---|---|---|
| `codigo_aula` | entero | Identificador externo; fue único en los 104 registros |
| `nombre_aula` | texto | Nombre del aula |
| `codigo_edificio` | entero | Identificador externo del edificio |
| `nombre_edificio` | texto | Nombre del edificio |
| `sillas` | entero | Capacidad |
| `codigo_dependencia` | entero | Identificador externo de la dependencia |
| `nombre_dependencia` | texto | Nombre de la dependencia |
| `codigo_tipo_aula` | entero | Identificador externo del tipo |
| `nombre_tipo_aula` | texto | Nombre del tipo |
| `view` | HTML | Metadato visual redundante; no se persistirá |
| `iconcls` | texto | Clase CSS; no se utilizará para inferir el tipo |

Una segunda petición con `page=2&start=25&limit=25` devolvió exactamente los mismos 104 objetos, en el mismo orden. Por tanto, en las peticiones verificadas no se aplicó el desplazamiento ni el límite solicitados. Esto no demuestra el comportamiento para todos los posibles parámetros; el adaptador deberá comprobar cantidad y códigos recibidos, no asumir paginación convencional.

## Eventos académicos

Se consultó el endpoint `listarEventAulaPublico` con los parámetros `startDate`, `endDate`, `page`, `start` y `limit`, incluyendo:

- `2026-09-27` a `2026-10-31`, intervalo suministrado en la actividad.
- `2026-10-01` a `2026-10-29`, intervalo vigente.
- `2026-08-01` a `2026-12-31` y `2026-01-01` a `2026-12-31`.
- Los años completos 2023, 2024 y 2025 como comprobaciones adicionales.

Todas esas respuestas fueron:

```json
{"success":true,"evts":[],"message":"","total":0}
```

La petición sin parámetros respondió con `success: true`, `evts: []`, `message: "Parametros invalidos"` y `total: 0`.

## Alcance de la evidencia

- Se comprobó acceso público sin cookies ni encabezados especiales mediante las herramientas utilizadas.
- No se obtuvo ningún objeto dentro de `evts`; por ello **no están confirmados** los nombres de campos de un evento, su identificador, su relación con el aula ni sus formatos de fecha y hora.
- Las respuestas vacías no prueban que el servicio esté fuera de funcionamiento ni que no existan eventos en otros intervalos.
- La sincronización de eventos debe validarse nuevamente cuando el endpoint devuelve registros antes de cerrar el adaptador definitivo.
