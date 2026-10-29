# Entregable 5: monolito modular frente a microservicios

## Decisión

AulaSmart debe desarrollarse como un **monolito modular**, con un único backend Spring Boot y una base PostgreSQL, organizado mediante Clean Architecture. No se justifica iniciar con microservicios.

La decisión se basa en el alcance actual: cuatro roles, consulta de aulas, disponibilidad, sincronización periódica, solicitudes y un asistente conversacional. Estas funciones comparten transacciones y reglas, especialmente al aprobar una solicitud sin conflictos.

## Evaluación en el contexto de AulaSmart

### Complejidad y tamaño

El proyecto tiene un número acotado de módulos y un equipo académico pequeño. Dividirlo en servicios exigiría definir contratos remotos, versiones, descubrimiento y propagación de errores antes de validar el producto. Un monolito modular permite concentrar el esfuerzo en las reglas y en la calidad de los datos.

### Consistencia y transacciones

La aprobación debe revisar horarios académicos y solicitudes aprobadas, registrar auditoría y evitar solapamientos. En el monolito estas operaciones pueden coordinarse en una transacción local y apoyarse en restricciones PostgreSQL. Con microservicios sería necesario diseñar consistencia entre servicios, compensaciones y tratamiento de respuestas parciales, sin un beneficio proporcional.

### Despliegue y operación

Un backend único requiere una canalización de construcción, configuración y despliegue. Microservicios multiplicarían artefactos, configuraciones, registros, monitoreo y posibles fallos de red. AulaSmart no presenta actualmente demanda ni equipos independientes que obliguen a asumir ese coste.

### Mantenimiento y evolución

La separación por módulos conserva responsabilidades reconocibles: `users`, `aulas`, `solicitudes`, `chatbot` e integraciones. Clean Architecture mantiene las reglas alejadas de REST, JPA y API externas. Así se obtiene capacidad de cambio sin distribuir el sistema.

### Pruebas

Las reglas de disponibilidad y aprobación pueden probarse como casos de uso con dobles de los puertos. Las restricciones y consultas se verifican contra PostgreSQL. En microservicios se añadirían pruebas de contrato y escenarios distribuidos, necesarios solo cuando esa distribución exista.

### Rendimiento y escalado

La consulta y la sincronización pueden optimizarse primero con índices, consultas apropiadas y configuración del único despliegue. No hay evidencia para afirmar que un módulo necesite escalar de forma independiente. Si más adelante la carga de IA o sincronización lo demuestra, se podrá extraer un componente detrás de sus puertos, sin convertir esa posibilidad en una obligación actual.

## Riesgos del monolito y mitigaciones

- **Monolito desorganizado:** evitarlo mediante límites de paquetes, APIs públicas de aplicación y revisión de dependencias.
- **Acoplamiento a infraestructura:** mantener dominio y casos de uso independientes de Spring MVC, JPA y clientes externos.
- **Crecimiento del esquema compartido:** asignar cada tabla a un módulo dueño y acceder a otros datos mediante contratos, no mediante consultas indiscriminadas.
- **Despliegues conjuntos:** mantener cambios pequeños, migraciones compatibles y pruebas automatizadas antes de integrar.
- **Dependencias circulares:** usar puertos cuando exista inversión real, como las ocupaciones aportadas por `solicitudes` a la disponibilidad de `aulas`.

Compartir un despliegue no significa mezclar responsabilidades: esa es la diferencia entre un monolito modular y un bloque sin estructura.

## Cuándo reconsiderar microservicios

La decisión debería revisarse solo ante evidencias concretas, por ejemplo: equipos que necesiten desplegar módulos de forma autónoma, cargas muy diferentes que exijan escalado independiente, requisitos de aislamiento que no puedan cumplirse en el proceso actual o ciclos de vida tecnológicos incompatibles. Una supuesta mayor modernidad no es criterio suficiente.

## Papel de Clean Architecture y los patrones

Clean Architecture es la organización interna; no convierte el monolito en microservicios ni exige capas repetitivas. Cada módulo tendrá solo las capas que aporten separación y pruebas útiles.

Los patrones se aplican a problemas concretos:

- **Adapter** para aislar SIGA y Gemini.
- **Repository** para encapsular persistencia.
- **Strategy** únicamente si aparecen criterios intercambiables demostrables.
- **Factory Method** únicamente si la creación o selección de implementaciones se vuelve compleja.

Esta combinación ofrece una solución verificable y mantenible para el alcance académico, sin sobreingeniería ni renunciar a una evolución futura ordenada.
