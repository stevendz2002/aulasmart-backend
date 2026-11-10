# Arquitectura y decisiones de AulaSmart

Estado vigente (2026-11-10): AulaBot publicado en main `1e7da77`. Corrección de solicitudes
aceptada en `feature/solicitudes-historial`, publicación en main autorizada. Las secciones anteriores de fase 5
describen su evolución; prevalece la actualización final de este documento.


Revisión: 2026-11-09, fase 4 implementada en `6965a7a`, aceptada para entrega vía develop/main.

## Mapa del sistema

- `aulasmart-backend/pom.xml`: Java 17, Spring Boot 3.5.7; Spring MVC, Security,
  JPA, Flyway y PostgreSQL. Migraciones en `src/main/resources/db/migration`.
- `aulas`: catálogo, disponibilidad y ocupación. Casos de uso dependen de puertos;
  adaptadores JPA consultan catálogo, horarios y estado de sincronización.
- `solicitudes`: registro pendiente, revisión y consulta por permisos. Conserva
  la restricción PostgreSQL EXCLUDE para impedir aprobaciones solapadas.
- `users`: rol único, BCrypt y JWT Bearer; sesiones/refresh en PostgreSQL (V10).
  Firma/validación con Spring Security/Nimbus; perfil y permisos se consultan por petición.
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
- Fase 4 aceptada: JWT y perfiles local/prod. El despliegue HTTPS real no se verificó.
- API relativa /api: proxy Angular en desarrollo, Nginx en Compose; sin secretos frontend.
- La interfaz conserva la advertencia CSS de consulta (10,77 kB / 6 kB), pendiente independiente.
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

## Decisiones de fase 1

Se reutilizan los casos de uso de solicitudes y el detalle de aulas. La confirmación
lleva el ID real a la bandeja mediante un parámetro de consulta; la API sigue filtrando
por identidad y rol. Las protecciones contra doble clic son de interfaz, sin cambiar
las garantías transaccionales del backend ni introducir reservas en SIGA.

## Decisiones de fase 4 (aceptada)

Se retira Basic y se conserva BCrypt. Access JWT HS256 por 15 minutos en memoria;
refresh opaco de 256 bits en cookie HttpOnly/SameSite=Strict, Secure en producción.
El servidor solo guarda hashes SHA-256 de refresh. La rotación bloquea la sesión
en PostgreSQL; reutilizar un token anterior revoca la familia completa. El cierre
revoca también access gracias a la comprobación de sesión y usuario en cada petición.
Esta consulta deliberada evita privilegios obsoletos y revocación diferida.

CSRF: las operaciones con cookie exigen Origin exacto y X-AulaSmart-CSRF: 1.
El resto de la API exige Bearer y no autentica usando cookies. Los endpoints de
autenticación no reciben el access anterior. Los perfiles local/prod separan HTTP
de desarrollo y HTTPS obligatorio; prod rechaza clave ausente y orígenes inseguros.
El servicio técnico de sesiones vive en infraestructura de seguridad, sin acoplar
los modelos de dominio a JWT, cookies ni JdbcTemplate. Detalles en fase-4.md.

## Fase 5: AulaBot (local, base 5e4b4da)

El módulo aulabot interpreta mensajes mediante un puerto y adaptador Groq. Un servicio
de aplicación reutiliza ConsultarAulasService y DisponibilidadAulaService, resuelve
catálogos y construye respuestas comprobadas. La presentación aplica cuota por
identidad y deriva acciones del rol autenticado. No hay escrituras de datos por IA.
La SPA usa dominio, puerto de aplicación y repositorio HTTP; contexto solo en memoria.
Límites de cuota de una instancia y privacidad descritos en docs/fase-5.md.

## Corrección de solicitudes (2026-11-10)

AulaBot publicado en `origin/main`: `1e7da77` (incluye `7db7cde`).
Corrección autorizada en `feature/solicitudes-historial`, base `1e7da77`; usuario aprobó cierre y publicación en main el 2026-11-10.
- Bandeja separada de activas e historial: últimas 50 históricas por ámbito de permisos,
  ordenadas por creación e ID descendente. Docentes solo ven las propias.
- Se conservan hasta 50 históricas por usuario en almacenamiento; las activas no se podan.
  Archivo lógico en la misma tabla al finalizar; los intervalos [inicio, fin) no bloquean
  reservas futuras. No se borra la constancia histórica para liberar el aula.
- Pendientes al llegar inicio: RECHAZADA, sin motivo adicional ni revisor humano ficticio.
  Limpieza automática cada segundo; lectura y revisión también aplican el límite temporal.
  Al reiniciar se procesan vencimientos acumulados. Interfaz se actualiza cada 15 segundos.
- V11 cambia solo la restricción de revisión para permitir rechazo automático sin persona;
  no migra datos ni modifica SIGA. No se borraron datos de la base instalada ni mocks globales.
- Usuario confirmó «quedó bien» y autorizó publicar la corrección el 2026-11-10.
