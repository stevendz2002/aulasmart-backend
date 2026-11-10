# Fase 5: AulaBot — plan e implementación local

Fecha: 2026-11-09. Base revisada: `main`, commit `5e4b4da`, árbol inicialmente limpio.
Estado: implementación autorizada, cambios locales en `feature/aulabot`, sin commit/push.
El usuario autorizó Groq, confirmó cuenta Free sin facturación y pidió continuar el
trabajo iniciado por Kimi. La publicación y aceptación final siguen pendientes. Fase 4 cerrada.

## Evidencia del estado actual

- `features/aulabot/aulabot-page.ts` contiene únicamente una pantalla informativa.
  La mención a SendChatMessageUseCase describe una intención, no una integración existente.
- `/aulabot` exige sesión iniciada. El dashboard y la barra lateral ya enlazan la vista.
- No se encontró módulo conversacional en el backend revisado.
- ConsultarAulasService permite filtros de edificio, tipo, dependencia y capacidad mínima.
- DisponibilidadAulaService combina eventos SIGA y solicitudes locales aprobadas;
  rechaza datos sin vigencia/cobertura suficiente. Debe seguir siendo la autoridad.
- Consulta ya tiene detalles, ocupación y formulario de solicitud con permisos.
  La navegación por aula/acción existe; trasladar también fechas requiere revisar
  y ampliar su contrato explícitamente, sin perder la franja de la conversación.

## Objetivo y alcance propuesto

Permitir consultar aulas en español, aclarar datos incompletos y pasar de un
resultado comprobado a sus detalles o al formulario existente de solicitud.

Ejemplo: «Necesito un aula para 40 personas mañana de 8 a 10 en el bloque B».
El asistente interpreta filtros, muestra la fecha completa y la franja en hora
Colombia, consulta el backend y devuelve aulas reales. «Mejor a las 10» debe
aclarar duración/final si no puede conservarlos inequívocamente.

Primera entrega propuesta:

1. Buscar aulas por nombre/código y filtros disponibles en el catálogo.
2. Consultar disponibilidad para fecha, inicio y fin, con aclaraciones cuando falten.
3. Continuar una consulta: cambiar capacidad, edificio, tipo o franja manteniendo
   solamente los filtros que el usuario no haya reemplazado.
4. Mostrar resultados compactos con capacidad, ubicación, estado, franja y momento
   de verificación, y acciones Ver aula / Solicitar según el rol.
5. Resolver preguntas básicas sobre cómo consultar o solicitar usando textos
   aprobados del producto, sin inventar políticas institucionales.

Fuera de esta entrega: crear o aprobar solicitudes automáticamente, escribir en
SIGA, administrar usuarios, consultar expedientes personales, voz, archivos y
búsqueda general por Internet. El usuario confirma cualquier solicitud en el
formulario existente; una aprobación local no es una reserva institucional.

## Diseño propuesto

- Feature Angular AulaBot organizada en presentación, aplicación e infraestructura,
  siguiendo el patrón existente. Chat con ejemplos iniciales, carga, error,
  reintento, nueva conversación y adaptación móvil; identidad azul/blanca.
- Módulo backend `aulabot` con caso de uso de envío de mensaje, contrato de consulta
  estructurada y adaptador a los casos de uso de aulas. No duplicar reglas SIGA
  ni consultar SQL generado por un modelo.
- Endpoint propuesto `POST /api/aulabot/mensajes`, autenticado con JWT. Respuesta
  estructurada: texto, filtros interpretados, aclaración requerida, resultados y
  acciones permitidas. Los identificadores y acciones se construyen en servidor.
- Si se elige IA, un puerto de interpretación aísla al proveedor. La IA interpreta
  lenguaje; el servidor valida fechas, filtros, permisos y datos. Disponibilidad,
  nombres e identificadores mostrados proceden de resultados reales.
- Contexto breve de conversación: filtros confirmados y últimos mensajes necesarios,
  con tamaño y caducidad limitados. Propuesta inicial sin historial persistente:
  se reinicia al recargar/cerrar sesión; cualquier contexto del cliente es entrada
  no confiable y se valida de nuevo. Persistencia histórica requeriría aprobación
  de retención y privacidad, y una migración si se decide incluirla.
- La pantalla de resultados recibe aula y franja explícitas. Al solicitar, se
  vuelve a validar disponibilidad; una respuesta anterior no garantiza reserva.

## Seguridad y operación

- Mismos roles que consulta; Solicitar solo para roles ya autorizados. El backend
  resuelve la identidad y nunca acepta un rol sugerido por la conversación.
- Pedir precisión ante «mañana», «en la tarde», aulas ambiguas o fechas fuera de
  cobertura. Mostrar siempre la interpretación temporal concreta antes de actuar.
- Diferenciar sin resultados, datos no verificables, servicio de IA no disponible
  y entrada incompleta. Ante caída, ofrecer acceso a Consulta de aulas.
- El contenido del usuario, catálogo y respuestas de IA no puede cambiar permisos,
  instrucciones del servidor ni habilitar herramientas arbitrarias.
- Si hay proveedor externo: enviar solo información mínima de la consulta; excluir
  JWT, cookies, contraseñas, correo y datos de solicitudes ajenas. Clave solo en backend.
- Límites de mensajes/tamaño por usuario, tiempo de espera, llamadas por consulta
  y presupuesto configurable. No registrar conversaciones completas por defecto.
- Proveedor elegido por el usuario: Groq, modelo openai/gpt-oss-120b, cuenta Free
  sin facturación confirmada. No se contrató ningún servicio de pago.

## Orden de trabajo tras aprobación

1. Acordar alcance y modalidad (IA libre o consultas guiadas), proveedor y presupuesto
   si corresponde; preparar ejemplos de aceptación. Rama propuesta: `feature/aulabot`.
2. Implementar contratos y backend reutilizando catálogo/disponibilidad, con pruebas
   deterministas para interpretación, contexto, permisos y errores.
3. Conectar el adaptador de lenguaje elegido y validar sus salidas; pruebas con
   respuestas controladas y una prueba real separada si se autoriza su consumo.
4. Implementar chat y acciones, conservar la franja al navegar y actualizar los
   avisos de AulaBot en el dashboard.
5. Ejecutar pruebas backend/frontend, compilaciones y prueba funcional por rol.
   Documentar resultados reales y límites; entregar local para prueba del usuario.
6. Publicar únicamente tras aprobación explícita. No iniciar otra fase automáticamente.

## Criterios de aceptación

- La misma consulta y franja producen los mismos resultados que Consulta de aulas.
- No se afirma disponibilidad con SIGA desactualizado o fuera de cobertura.
- Datos incompletos y nombres ambiguos generan una pregunta concreta, no una suposición oculta.
- Cambiar un filtro mantiene el resto correctamente; nueva conversación borra contexto.
- Ver aula abre el ID correcto; Solicitar conserva la franja y exige confirmación.
- Un estudiante no obtiene capacidad de solicitud por pedirla en el chat.
- Instrucciones maliciosas no permiten saltar permisos ni acceder a información ajena.
- Límites, errores y vencimiento de sesión no dejan envíos duplicados ni respuestas
  antiguas asociadas a otra conversación/identidad.
- Pruebas verifican filtros, fechas en America/Bogota, contexto, caída de dependencias,
  acciones y accesibilidad básica. No se requiere SIGA vivo para pruebas automáticas.

## Decisiones pendientes

Groq + GPT-OSS 120B aprobados para implementar. Presupuesto API: cero.
Pendiente prueba del usuario y autorización de publicación; no se autoriza facturación,
compra de créditos, infraestructura de pago ni cambio automático a modelos de pago.

## Evaluación de costo cero (2026-11-09)

- Selección autorizada: GPT-OSS 120B mediante el plan gratuito de Groq,
  sujeto a disponibilidad y a verificar los límites de la cuenta al configurarla.
  La documentación consultada publica 30 solicitudes/minuto, 1.000/día,
  8.000 tokens/minuto y 200.000 tokens/día; son cuotas compartidas por organización,
  no por usuario de AulaSmart. El primer límite alcanzado detiene el consumo.
- GPT-OSS tiene pesos abiertos, pero ejecutarlo no implica infraestructura gratis:
  OpenAI describe 120B en una GPU de 80 GB y 20B con 16 GB de memoria.
  La variante 20B local solo es alternativa tras verificar equipo, latencia y recursos
  disponibles; electricidad y alojamiento no están cubiertos por la licencia.
- OpenRouter ofrece variantes gratuitas, sujetas a disponibilidad y cuotas. No se
  presupone que GPT-OSS 120B gratuito esté disponible permanentemente allí.
- Diseño con presupuesto API cero: cuenta gratuita, sin activar plan de pago;
  modelo/proveedor explícitos, contexto breve y sin reintentos ilimitados. Al agotar
  cuota, mostrar aviso y acceso a la consulta convencional; nunca pasar a pago.
- Costo API cero es distinto de garantizar operación ilimitada o alojamiento gratis
  de toda AulaSmart. Si cambian las condiciones, desactivar integración antes de pagar.
- Validar calidad en español, interpretación temporal y consultas estructuradas con
  casos reales de AulaSmart antes de fijar modelo. No se ha medido su calidad aún.

Fuentes consultadas:
- [Límites oficiales de Groq](https://console.groq.com/docs/rate-limits).
- [GPT-OSS: requisitos y licencia](https://openai.com/index/introducing-gpt-oss/).
- [Límites de OpenRouter](https://openrouter.ai/docs/api_reference/limits).

## Implementación local (2026-11-09)

Se conservaron el puerto y modelos útiles iniciados por Kimi, ampliándolos para catálogo
y filtros de nombre/dependencia. Se retiró su intérprete regex incompleto y un DTO sin
uso: el modo activo es Groq y no existe sustitución silenciosa por otro proveedor.

Una llamada estructurada interpreta hasta seis mensajes breves. La respuesta visible
se construye en el servidor: textos de aclaración controlados, IDs del catálogo y
disponibilidad mediante los casos de uso existentes. No se ejecuta SQL ni herramientas
propuestas por el modelo. Edificios/tipos ambiguos se aclaran, no se ignoran.

Chat con carga/error/reintento, ejemplos, tarjetas, Ver aula y Solicitar por rol.
La franja ISO se convierte a Colombia al navegar a Consulta y se preserva en el
formulario. El servidor valida nuevamente al registrar; no hay solicitudes automáticas.

Contexto solo en memoria: resumen de filtros vigente más aclaraciones recientes,
hasta cinco entradas de 600 caracteres, mensaje nuevo hasta 600. El resumen usa
fecha y horas explícitas de Colombia para evitar conversiones temporales por el modelo. Se reinicia al
recargar, nueva conversación, cambio de identidad o tras 30 minutos sin actividad.
Se cancelan peticiones al salir/resetear. Se muestran hasta 10 aulas por respuesta.

Límites locales de una instancia: 2 llamadas por usuario/minuto, 3 globales/minuto,
50 globales/día UTC; las propiedades aulasmart.aulabot.minute-limit y day-limit
permiten reducirlos. La cuota se reinicia al reiniciar el backend; no es contabilidad
distribuida ni sustituye los límites de Groq. La cuenta debe permanecer en Free.
Una sola llamada por mensaje, sin reintentos automáticos, timeout 20 s y máximo
900 tokens de salida. Sin cambio a pago. Ante error se ofrece Consulta de aulas.

Configuración: GROQ_API_KEY y AULASMART_AULABOT_ENABLED en .env local ignorado;
clave proporcionada por el usuario, no versionada. Deshabilitado por defecto en
.env.example. No se registran cuerpos de mensajes ni respuestas externas. Se omiten
identidad, JWT y cookies; además se filtran patrones de correos/claves/tokens en texto.
El usuario debe evitar escribir datos personales: el filtro no garantiza anonimización
completa de texto libre. No se consultó SIGA vivo ni se modificó la base instalada.

La clave compartida en el chat debe reemplazarse después de las pruebas. No aparece
en este documento. Reiniciar backend para cargar .env; probar AulaBot desde la barra
lateral. No se necesita migración de base de datos en esta fase.

Validación real de Groq: prueba opt-in con tres mensajes sintéticos aprobada
(consulta inicial, cambio a 50 personas conservando franja y aclaración de una
hora incompleta). Se detectó y corrigió pérdida de contexto temporal al enviar UTC;
el resumen ahora usa fecha/hora Colombia explícitas. No se envió catálogo ni datos
personales a Groq en estas pruebas.

Validación final comprobada:
- Backend: mvnw.cmd -o verify con PostgreSQL 17 descartable: 125 pruebas
  aprobadas, cero fallos/errores. La prueba externa opt-in se omitió en esta
  corrida para no repetir llamadas; se ejecutó y aprobó por separado.
- GroqConexionTest: una prueba con tres llamadas reales sintéticas aprobada.
- Frontend: 99 pruebas aprobadas en 22 archivos; compilación de producción correcta.
- Paquete backend generado. Configuración Compose validada con .env.example.
- git diff --check correcto; búsqueda de claves Groq en fuentes/docs sin coincidencias.
- .env confirmado ignorado por Git. Base descartable detenida.
- Persiste advertencia CSS previa de Consulta: 10,77 kB frente a presupuesto de 6 kB.
Prueba visual/funcional instalada del usuario pendiente; no se declara fase cerrada.

## Revisión autorizada 5A–5E (2026-11-10)

El usuario autorizó revisar la fase 5 en cinco sub-fases sobre el mismo árbol
local sin commit. Continúa pendiente su prueba y la autorización de publicación.

### 5A Memoria en servidor
- Nuevos `SolicitudInterpretacion`, `EstadoConversacionAulaBot` y
  `RegistroConversacionesPort`, con implementación `ConversacionesEnMemoriaAdapter`
  por usuario (sin Redis): TTL de contexto 30 min (`aulasmart.aulabot.contexto-ttl`),
  deduplicación 90 s (`aulasmart.aulabot.deduplicacion-ttl`), máximo 500 usuarios
  y 8 interpretaciones recientes por usuario.
- `AulaBotService.enviar(usuario, reiniciar, mensaje)`: el cliente ya no envía
  historial. `MensajeAulaBotRequest` es `{mensaje, nuevaConversacion}`.
- Al reproducirse un mensaje idéntico dentro de la ventana corta se reutiliza la
  interpretación, pero la disponibilidad se recalcula siempre con datos frescos.
- Si la consulta completa produce una aclaración conocida (edificio ambiguo,
  equipamiento desconocido), el servidor guarda esa pregunta como pendiente y
  conserva la consulta vigente; así el siguiente mensaje del usuario la completa.
- Los resultados de disponibilidad nunca se guardan; la memoria solo guarda
  filtros y pregunta pendiente.
- Frontend: envía `{mensaje, nuevaConversacion}`; la señal de reinicio se marca
  al «Nueva conversación», cambiar identidad o tras 30 minutos de inactividad.
  Se eliminó el resumen de filtros que el cliente reenviaba a Groq.

### 5B Equipamiento (videobeam)
- `FiltroAulaBot` incorpora `equipamiento`; el esquema de Groq pide copiar el
  término tal como lo escribió el usuario, sin traducirlo.
- Nuevo `EquipamientoAula` (VIDEOBEAM, COMPUTO, AUDIO, INTERACTIVA) que resuelve
  contra los nombres reales del catálogo SIGA (p. ej. "B101-PANTALLA"); no hay
  tabla de equipos ni migración de base de datos.
- Término de equipo desconocido reproduce una aclaración que lista solo el
  equipamiento realmente evidente en el catálogo actual; no se inventa nada.
- «sin videobeam» elimina el filtro en el siguiente turno; la memoria conserva
  el resto de filtros.

### 5C Alternativas ante búsqueda sin resultados
- Nuevo estado `SIN_RESULTADOS` con acciones sugeridas y verificadas con
  consultas locales reales (sin IA ni cuota): quitar el bloque, bajar la
  capacidad, quitar el requisito de equipamiento y probar otra franja del mismo
  día (±2 h, dentro de 06:00–22:00 y futura), con conteos reales en las etiquetas
  ("— hay N libres a esa hora"). Máximo 3 consultas locales extra.
- Con filtro por nombre solo se ofrece «Busca sin especificar el aula exacta»:
  los conteos combinados con nombre no se pueden mostrar honestamente.
- El frontend muestra chips con esas acciones; cada clic envía el texto como
  mensaje nuevo (una llamada a Groq por clic: se mantiene la vía única).

### 5D Chat único
- Se eliminó por completo el modo guiado: endpoint `POST /api/aulabot/consulta-guiada`,
  matcher de seguridad correspondiente, `ConsultaGuiadaRequest`,
  `ConsultaGuiadaTest`, `domain/guided-query` del frontend y su especificación.
  `AulaBotService.consultar` dejó de ser público (uso interno de `enviar`).
- Un solo chat: chips de bienvenida que se envían directamente («Buscar aulas
  disponibles», «Necesito un aula para 30 personas», «Buscar aulas con videobeam»,
  «¿Cómo solicito un aula?»), chips contextuales por estado (aclaración de fecha
  → Hoy/Mañana; de horario → franjas; catálogo consultado → hoy/mañana 08–10;
  sin resultados → acciones de 5C) y el botón «Nueva conversación».

### Validación (2026-11-10)
- Backend: `mvnw.cmd -o verify` con PostgreSQL 17 descartable (puerto 55439):
  139 pruebas aprobadas, cero fallos/errores; una omitida por opt-in
  (GroqConexionTest). Base descartable detenida; la base instalada no se tocó.
- Frontend: 99 pruebas aprobadas; compilación de producción correcta (persiste
  la advertencia CSS previa de consulta-page, documentada como mantenimiento).
- Compose validado. Sin migraciones nuevas en esta revisión.
- GroqConexionTest quedó actualizado al flujo `SolicitudInterpretacion`
  (consulta inicial, seguimiento con consulta vigente entregada por el servidor
  y aclaración de «Mejor a las 10»); no se ejecutó en el verify. Ejecutar solo
  si el usuario lo autoriza con `AULASMART_GROQ_SMOKE=true`.

### Corrección tras la prueba del usuario (2026-11-10)

La prueba del usuario reportó tres fallas: no lograba llegar a una solicitud,
la memoria no se conservaba y el bot «no sabía lo que pedía». Se reprodujeron
en vivo con el backend real y PostgreSQL descartable (puerto 8088; nunca la
base instalada del usuario) y se corrigieron sus causas:

1. La aclaración descartaba lo que la IA ya había entendido: en la rama
   ACLARACION el servidor guardaba solo la pregunta y volvía «filtros: null»
   (comprobado en la reproducción), de modo que el turno siguiente partía sin
   contexto. Corrección: `InterpretacionAulaBot` anexa el filtro parcial
   entendido (`parcial`), `GroqInterpretador` lo decodifica en ACLARAR/AYUDA y
   también cuando la franja es inválida, y `AulaBotService` lo mezcla campo a
   campo con la consulta vigente (lo no mencionado se conserva; los datos
   ausentes no se inventan). La respuesta ACLARACION muestra en `filtros` lo ya
   entendido.
2. La deduplicación ignoraba el estado: el mismo mensaje repetido tras cambiar
   el contexto reutilizaba la interpretación vieja. Corrección:
   `RegistroConversacionesPort` firma el estado (consulta vigente + aclaración
   pendiente) al registrar cada turno y la reutilización solo vale para el mismo
   mensaje con la misma firma; el registro queda contra el estado resultante del
   turno, por lo que repetir con otro contexto se interpreta de nuevo.
3. NO_VERIFICABLE quedaba en callejón sin salida. Corrección: se ofrece la
   acción «Busca en el catálogo sin horario» y el frontend presenta chips en
   NO_VERIFICABLE igual que en SIN_RESULTADOS, con lo que se llega al catálogo y
   al botón Solicitar.

Validación de la corrección (mismo árbol sin commits):
- Backend: `mvnw.cmd -o verify` con PostgreSQL 17 descartable reconstruido
  limpio (la primera corrida falló por filas residuales de los logins de
  diagnóstico en la base descartable; recreada, pasó): 143 pruebas aprobadas,
  0 fallos/errores, 1 omitida por opt-in (GroqConexionTest).
- Frontend: 99 pruebas y compilación de producción correctas (persiste la
  advertencia CSS previa de consulta-page, documentada como mantenimiento).
- Reproducción en vivo con el jar recompilado y la base descartable (cuenta
  docente de prueba; llamadas reales a Groq con cuota propia; la clave nunca se
  leyó ni imprimió): mensaje 1 «...30 personas en el bloque B, que tenga
  videobeam» → ACLARACION pidiendo la fecha y `filtros` con capacidad 30,
  Bloque B y videobeam; mensaje 2 «mañana de 8 a 10» → NO_VERIFICABLE honesto
  conservando los tres filtros y el chip del catálogo; mensaje 3 «Busca en el
  catálogo sin horario» → CATALOGO con total 1 (B101-PANTALLA, grupo videobeam)
  y `puedeSolicitar` true. Backend y base descartable detenidos al terminar.

### Chat informativo pregunta a pregunta (2026-11-10)

Tras la prueba funcional del usuario, la decisión fue descartar la gestión de
solicitudes desde el chat. El chat quedó estrictamente informativo:

- Cada mensaje se interpreta de forma independiente: sin memoria de turnos, sin
  filtros vigentes ni aclaraciones pendientes entre turnos. Las frases incompleta
  (sin fecha y horario) reciben una aclaración que pide escribir la consulta en
  una sola frase completa (ej.: «aula para 30 personas en el bloque B mañana de
  08:00 a 10:00»); nunca se adivinan ni se inventan datos.
- Se eliminó el contexto conversacional completo (`EstadoConversacionAulaBot`,
  consulta vigente, pendiente, mezcla de parciales) y la deduplicación pasó a
  considerar solo el texto del mensaje en una ventana corta (90 s, 500 usuarios).
- Se eliminó del contrato del endpoint (`POST /api/aulabot/mensajes`) la respuesta
  de canales de solicitud: ya no se emite `puedeSolicitar` ni `acciones`.
- Los textos del bot remiten al uso informativo (catálogo, capacidad, equipamiento,
  disponibilidad) y a la sección Solicitudes cuando se habla de reservas.

Validación de esta corrección:
- Backend: `mvnw.cmd -o verify` sobre PostgreSQL 17 descartable reconstruido
  limpio: BUILD SUCCESS, 137 pruebas aprobadas, 0 fallos/errores, 1 omitida por
  opt-in (GroqConexionTest).
- Frontend: 98 pruebas aprobadas y compilación de producción correcta.
- Reproducción en vivo con el jar de esta revisión: las respuestas salen sin
  `puedeSolicitar`/`acciones`; la frase incompleta produce una aclaración con
  ejemplo completo; la frase completa devuelve catálogo/disponibilidad honesta.
- Backend y base descartable detenidos.

### IA determinista sin proveedor (2026-11-10)

Tras pedir «solo informativo» y luego «totalmente determinista, dejar a un lado Groq»:

- `GroqInterpretador` dejó de ser el bean primario de `InterpretacionLenguajePort`;
  la interpretación ya no depende de red ni de proveedor.
- Nuevo `InterpretacionDeterminista` (mismo puerto) construye consultas desde el
  texto con reglas locales: ayuda → ACLARACIÓN con texto informativo; catálogo →
  CATALOGO; fecha + franja válidas → DISPONIBILIDAD; resto → ACLARACIÓN con ejemplo
  de frase completa. Resultados idénticos para la misma entrada.
- Validación: suite completa `mvnw.cmd -o verify` BUILD SUCCESS (137 pruebas, 0
  fallos, 1 omitida opt-in Groq), repro en vivo con el jar determinista y respuestas
  reproducibles sin consumo de cuota del proveedor.

### Arquitectura híbrida determinista con Groq (2026-11-10)

Tras la petición del usuario se reintroduce Groq con una única responsabilidad
(extraer parámetros JSON), y el backend recupera toda la lógica:

- `SolicitudInterpretacion` lleva mensaje, consulta vigente, aclaración pendiente
  y `FranjaAulaBot` pendiente. `GroqInterpretador` devuelve JSON validado por
  esquema; las fechas relativas (`fechaRelativa` HOY/MANANA) se resuelven en el
  backend con `America/Bogota`.
- `AulaBotService` es la máquina de estados determinista: mezcla filtros campo a
  campo (`mezclar`) y franja (`combinarFranja`), pregunta solo el dato faltante
  (`preguntaFaltante`) y consulta disponibilidad con los casos de uso reales en
  cuanto la franja queda completa, incluso si fecha y horario llegan en turnos
  distintos. Respuestas por plantillas del backend.
- Acciones estructuradas `AccionAulaBot` (BUSCAR_AULA, CONSULTAR_DISPONIBILIDAD,
  CAMBIAR_BLOQUE, CAMBIAR_HORARIO, VER_OPCIONES) ejecutadas sin Groq; el contrato
  añade `sugerencias` (sin `puedeSolicitar` ni `acciones`).
- Cuota corregida: 6 mensajes/minuto por usuario y 20/minuto global (configurable),
  acciones estructuradas sin consumo; el límite o la caída del intérprete ya no
  devuelven 503 sino una respuesta guiada conservando el contexto.
- Validación: `mvnw.cmd -o verify` BUILD SUCCESS, 146 pruebas backend (incluida
  la secuencia fecha/horario en turnos distintos), 99 frontend, build de
  producción correcto.

### Pendiente antes de publicar
- Prueba funcional del usuario: conversación en varios turnos («mejor para 50»,
  «sí, busca en el C», «sin videobeam», «mejor a las 2»), filtros combinados,
  sin resultados con chips, nueva conversación y permisos por rol.
- Reemplazar la clave Groq expuesta en el chat por una nueva (solo en `.env`).
- Autorización de commit/push de esta revisión.
