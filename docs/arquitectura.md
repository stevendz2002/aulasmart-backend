# Arquitectura y decisiones de AulaSmart

Revisión: 2026-11-08, `feature/single-user-role`, fase 2 local sobre `7030f16`.

## Mapa del sistema

- `aulasmart-backend/pom.xml`: Java 17, Spring Boot 3.5.7; Spring MVC, Security,
  JPA, Flyway y PostgreSQL. Migraciones en `src/main/resources/db/migration`.
- `aulas`: catálogo, disponibilidad y ocupación. Casos de uso dependen de puertos;
  adaptadores JPA consultan catálogo, horarios y estado de sincronización.
- `solicitudes`: registro pendiente, revisión y consulta por permisos. Conserva
  la restricción PostgreSQL EXCLUDE para impedir aprobaciones solapadas.
- `users`: cuentas persistidas con un único rol obligatorio, BCrypt y Basic.
  JWT pertenece a la fase 4.
- `integrations/siga`: consulta HTTP de catálogo/eventos, sincronización inicial,
  manual y programada. No hay escritura de reservas institucionales implementada.
- `shared/presentation`: traducción central de errores a respuestas HTTP.
- `aulasmart-frontend`: Angular 21.2, TypeScript 5.9 y pruebas Vitest; features
  `aulas`, `solicitudes`, `usuarios`, sesión, dashboard y AulaBot.
- `compose.yaml`: PostgreSQL, backend y frontend; `.env` local ignorado.

## Decisiones de fase 0

**Disponibilidad verificable.** La búsqueda y el detalle exigen cobertura temporal
completa, sincronización reciente y última corrida de eventos exitosa. Los eventos
deben haberse consultado después de actualizar el catálogo. Durante una corrida
o tras una falla se prefiere un estado no verificable a afirmar disponibilidad.
Las lecturas usan una instantánea transaccional consistente.

**Cobertura persistida.** V8 agrega límites instantáneos de cobertura; no modifica
V1–V7. Los registros antiguos con cobertura desconocida requieren resincronización.
La ventana de sincronización configura un horizonte, no una prueba de disponibilidad
fuera de ese horizonte. Una consulta vacía solo significa libre cuando pasó la validación.

**Ocupación unificada.** El detalle combina SIGA y solicitudes aprobadas localmente.
Las reservas se muestran con etiqueta genérica, sin divulgar motivos ni usuarios.
Los fallos y respuestas atrasadas no deben generar franjas libres.

**Concurrencia.** V8 agrega `version` a solicitudes; JPA usa bloqueo optimista.
Una revisión concurrente perdedora recibe conflicto. EXCLUDE sigue protegiendo
solicitudes distintas con el mismo aula y horario. Los conflictos se traducen a 409.

**Límites arquitectónicos.** `ConsultaSigaPort` desacopla los sincronizadores del
cliente HTTP. `PrincipalDeSolicitudes` recibe el correo, no un objeto de Spring
Security. No se eliminan anotaciones de servicios/transacciones sin beneficio real.

**Seguridad transitoria.** Basic permanece para no adelantar JWT. Sus credenciales
solo viven en memoria de la SPA; se eliminan valores persistidos de versiones anteriores.
El interceptor solo las adjunta a la API configurada. Cuentas con dominio de prueba
requieren `AULASMART_ALLOW_TEST_ACCOUNTS=true`, deshabilitado por defecto.

## Límites y decisiones diferidas

- Aprobación local no equivale a reserva confirmada en UCEVA; una consulta SIGA no
  puede impedir cambios institucionales posteriores.
- La sincronización está coordinada para una instancia del monolito; un despliegue
  con varias réplicas necesita coordinación distribuida antes de habilitarlo.
- Falta la fase de JWT/HTTPS/configuración definitiva y revisión CSRF. Basic con
  contraseñas de prueba habilitadas es exclusivamente para desarrollo controlado.
- La URL de API del frontend sigue siendo local; adaptarla al despliegue en fase 4.
- La interfaz conserva el presupuesto CSS excedido previo, a tratar en fase 3.
- V9 exige preflight y decisiones explícitas si una base contiene cuentas sin rol o con varios.

## Decisiones de fase 2

- `usuario.id_rol` obligatorio reemplaza la relación múltiple. V9 preserva
  `usuario_rol_historial_v9`; el historial no interviene en permisos.
- El contrato de API y sesión usa `rol` singular; el frontend usa `role` internamente.
  Se rechazan campos heredados `roles`, arrays, rol vacío y null explícito.
- Las ediciones bloquean la fila ADMINISTRADOR durante la transacción para
  impedir que cambios concurrentes eliminen al último administrador habilitado.
  Las cuentas sintéticas deshabilitadas por configuración no cuentan como acceso disponible.
- La edición de la cuenta propia actualiza el rol de su sesión en memoria.
