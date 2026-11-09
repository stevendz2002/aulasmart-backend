# Fase 4: configuración y autenticación

Fecha: 2026-11-09. Implementación: `6965a7a`, rama de origen `feature/jwt-authentication`.
Estado: fase aceptada y cierre autorizado por el usuario.
El usuario autorizó integrar y publicar los cambios en `develop` y después en `main`.
Este documento acompaña la entrega; verificar las referencias remotas para comprobar la publicación.
La corrección SIGA anterior sí se publicó en `origin/refactor/classroom-search-ui`.

## Cambios y decisiones

- Se retira HTTP Basic. `POST /api/auth/login` recibe correo/password y verifica
  BCrypt mediante el proveedor de Spring Security. No se cambian cuentas ni claves.
- Access JWT HS256, 15 minutos por defecto, validado por Spring Security/Nimbus:
  firma, expiración, emisor y audiencia `aulasmart-api`. Incluye rol único y sesión.
  La API verifica además sesión no revocada y cuenta activa con su rol vigente.
- Refresh opaco aleatorio de 256 bits, cookie `aulasmart_refresh`, HttpOnly,
  SameSite=Strict, Path=/api/auth; Secure en producción. No se devuelve en JSON.
  Duración absoluta de siete días; renovar no extiende indefinidamente la sesión.
- V10 añade sesiones y hashes SHA-256 de refresh. No altera usuarios ni reservas.
  Cada renovación bloquea la fila de sesión y consume el hash anterior. Detectar
  su reutilización revoca la familia completa, incluidos los access emitidos.
- Logout revoca en servidor y borra la cookie. La interfaz solo confirma el cierre
  después del éxito; si falla la red muestra un mensaje y permite reintentar.
- Access permanece solo en memoria. Al recargar se intenta restaurar con refresh;
  ante 401 se renueva y reintenta una vez. Peticiones simultáneas comparten renovación.
  Web Locks coordina pestañas cuando está disponible. Sin Web Locks, una carrera
  entre pestañas puede cerrar la sesión por detección de reutilización; no permite
  dos renovaciones válidas. Respuestas antiguas no restauran otra identidad.
- CSRF: login/refresh/logout exigen Origin exactamente permitido y cabecera
  X-AulaSmart-CSRF: 1; se rechazan origen ausente/ajeno y peticiones simples.
  El resto de la API usa exclusivamente Bearer y no autentica mediante cookie.
  No configurar comodines ni orígenes de terceros no confiables.
- Respuestas de autenticación no se cachean. Access no se adjunta a login/refresh
  ni a destinos externos. Las operaciones de autenticación tienen límite de 15 s.
- Limpieza diaria de sesiones expiradas, con eliminación en cascada de sus hashes.

## Entornos

`.env` y certificados quedan ignorados; `.env.example` solo contiene ejemplos.
No se leyó ni modificó el `.env` privado del usuario.

**Local:** perfil predeterminado `local`, HTTP localhost. Sin clave configurada se
genera una clave aleatoria efímera de firma por arranque. Reiniciar invalida access
anteriores; una cookie refresh vigente puede renovar con la nueva clave. Para
persistencia de firma, configurar AULASMART_JWT_SECRET con Base64 de 32 bytes
aleatorios como mínimo, generado con una herramienta criptográfica local.

**Producción:** perfil `prod`, clave persistente obligatoria, orígenes HTTPS
explícitos y cookies Secure. No admite cuentas sintéticas. Requiere DB_URL,
DB_USERNAME y DB_PASSWORD; el backend rechaza peticiones HTTP. Usa encabezados
reenviados exclusivamente detrás del proxy confiable, sin exposición pública
directa. Las publicaciones de PostgreSQL/backend en Compose se limitan a loopback.

`compose.prod.yaml` agrega TLS en Nginx, certificado propio en
`certs/fullchain.pem` y clave privada en `certs/privkey.pem` (no versionados).
Configurar AULASMART_CORS_ORIGINS con el origen público exacto, incluidos puertos
si no son los predeterminados. Ejecutar Compose con ambos archivos, base y prod.
El proxy HTTPS establece X-Forwarded-Proto y añade HSTS. El despliegue real y la
validez/renovación de certificados dependen del entorno, no se han ejecutado aquí.

Frontend usa `/api` del mismo origen. `npm start` utiliza `proxy.conf.json` hacia
localhost:8080; Compose usa Nginx. Reiniciar Angular al aplicar la configuración
del proxy. No hay URLs locales fijas ni secretos dentro del paquete del frontend.

## Validación

- Backend: `mvnw.cmd -o verify`, 109 pruebas aprobadas, cero fallos/errores/omisiones;
  paquete generado. PostgreSQL 17 descartable, migración V10 aplicada allí.
- Pruebas nuevas: login/cookies, credenciales inválidas, Basic rechazado, CSRF,
  rotación/reutilización, logout, cambio de rol/desactivación, firma/expiración/
  emisor/audiencia/sesión inválidos, refresh expirado y concurrencia real.
- Pruebas de configuración rechazan claves ausentes en prod, cortas o inválidas.
- Frontend: 91 pruebas aprobadas en 21 archivos; compilación de producción correcta.
- Compose local y prod: validación de configuración correcta con .env.example y
  variables de prueba, sin levantar servicios ni leer secretos privados.
- git diff --check correcto; PostgreSQL descartable detenido al terminar.
- No se usó la base instalada del usuario ni se consultó SIGA vivo. No se afirma
  validación manual de navegador ni de un despliegue HTTPS real.

## Guía de verificación instalada

1. Reiniciar backend e interfaz. V10 se aplica al arrancar el backend mediante
   Flyway; añade tablas, sin cambiar usuarios ni datos académicos.
2. Ingresar con las cuentas habituales. Verificar consulta, detalle y solicitudes
   según rol. Las cuentas de prueba siguen requiriendo la opción local existente.
3. Recargar la página: debe conservarse la sesión mediante renovación automática.
4. Mantener abierta más de 15 minutos y consultar: debe renovar sin pedir la clave.
5. Cerrar sesión, recargar y comprobar que ya no permite páginas protegidas.
6. Registrar cualquier incidencia posterior como mantenimiento; la publicación ya fue autorizada.

Cierre de fase aprobado explícitamente por el usuario el 2026-11-09. No se inicia otra fase.
Límites de la evidencia: no se verificó SIGA vivo ni el despliegue HTTPS con certificados
propios. La aprobación no se presenta como una ejecución manual realizada por el agente.
Advertencia CSS previa de consulta: 10,77 kB / 6 kB; pendiente de mantenimiento.

## Referencias de implementación

- [Spring Security: JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
- [OWASP: prevención CSRF, cabeceras y validación de origen](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).
