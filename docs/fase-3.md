# Fase 3: tarjetas de aulas

Revisión: 2026-11-09. Rama: `refactor/classroom-search-ui`, base `7c5779f`.
Estado: implementación local del diseño aprobado; pruebas y compilación correctas.

## Alcance aprobado

Solo tarjetas de resultados, manteniendo identidad azul/blanca. El usuario aprobó
la propuesta de mayor jerarquía visual y pidió cinco tarjetas por fila. Filtros,
diálogos, navegación, permisos y comportamiento de solicitudes quedan intactos.
La propuesta inicial de rediseño de toda la pantalla fue sustituida por este alcance.

## Implementación

- Cinco columnas en escritorio desde 1200 px; tres por debajo, dos desde 521 a
  800 px y una hasta 520 px. Nunca se aumenta a seis en pantallas grandes.
- Nombre protagonista y código numérico final en línea de referencia separada.
  Se conserva el nombre original en el modelo para búsqueda y seguimiento.
  Nombres con paréntesis descriptivos se mantienen completos, sin inventar códigos.
- Capacidad destacada, ubicación una vez y características reales del catálogo.
- Iconos SVG uniformes en lugar de emojis, sin dependencias adicionales.
- Por la última indicación del usuario: Libre verde y Ocupada rojo,
  con fondo suave y punto de estado como la referencia. Sin bordes laterales ni
  atenuar tarjetas ocupadas. Sin verificar permanece neutro.
- Botones según referencia: en libres Solicitar azul y Ver aula delineado; en
  ocupadas/sin verificar Ver aula azul y Solicitar delineado. Si el usuario no
  puede solicitar, Ver aula sigue principal. Permisos y acciones no cambian.
- Botones de 44 px, textos ajustables, foco visible y efecto hover discreto;
  se respetan las preferencias de movimiento reducido.

## Validación y pendientes

- Se añaden dos regresiones del modelo: separar código sin perder búsqueda y
  conservar nombres descriptivos. Suite: 76 pruebas aprobadas en 19 archivos.
- Build de producción correcto; persiste advertencia CSS: 9,42 kB frente a 6 kB.
- `git diff --check`: correcto.
- No se elevan presupuestos CSS para ocultar advertencias.
- No se realizó inspección visual en navegador; revisar en escritorio y móvil.
- Cambios locales sin publicación; fase 4 no autorizada.

## Corrección de referencia visual: iconos

El usuario aclaró que pedía iconos, no fotos. Se retiran la fotografía genérica,
su etiqueta, estilos y recurso descargado. Cabecera con icono azul sobre fondo
azul suave: pantalla genérica, proyector para audiovisual y pantalla interactiva
según el tipo existente. Iconos decorativos; el tipo continúa expresado como texto.
También se añaden iconos de ojo/calendario a las acciones y fondo suave a capacidad.
Se conservan cinco columnas y comportamiento existente.
Compilación correcta; advertencia CSS vigente: 10,06 kB frente a 6 kB.
Sin prueba visual en navegador; `git diff --check` correcto.

## Tipografía y estados según referencia

Última corrección: estados verde/rojo explícitamente solicitados. Nombres y ubicación
en mayúsculas se convierten a formato legible; características en estilo oración.
Se preservan códigos alfanuméricos, siglas conocidas y el nombre original para búsqueda.
Los textos que ya usan mayúsculas/minúsculas se mantienen intactos.
Validación: 77 pruebas aprobadas en 19 archivos; compilación correcta y diff sin
errores de formato. Advertencia CSS: 10,41 kB / 6 kB; presupuesto no modificado.

## Ajuste a la plantilla: estados, botones y pasteles

- El estado visible se acorta a Ocupada; Libre verde y Ocupada rojo con punto
  y fondo pastel. Sin verificar conserva su distinción neutra.
- Icono principal sobre azul pastel (#eaf0ff), etiquetas lavanda (#eef2ff) y
  capacidad sobre gris azulado muy claro (#f5f7fc). Cambios limitados a tarjetas.
- Botones de 44 px con radio de 10 px, iconos ojo/calendario y jerarquía condicional
  descrita arriba. Se conserva la cuadrícula de cinco columnas.
- Capacidad en una línea, como la referencia; etiquetas con iconos discretos.
- No se modifica el backend ni se crean dependencias. Validación: 77 pruebas
  aprobadas, build correcto; advertencia CSS de 10,71 kB / 6 kB, sin elevar
  presupuesto. No se realizó comparación visual en navegador.

## Ajuste fino de capacidad

Por solicitud del usuario, el número de capacidad baja de 1,2 rem a 1,05 rem
(aproximadamente 2,4 px / 1,8 pt con raíz de 16 px). Solo cambia ese tamaño.
Verificación: revisión de la regla y git diff --check; sin repetir suite por este ajuste CSS.

## Aprobación y continuación

El usuario aceptó el diseño actual y autorizó commits y publicación el 2026-11-09.
Después de publicar, autorizó corregir interacción de búsqueda principal y mejorar
legibilidad del buscador de consulta conservando su ancho.
