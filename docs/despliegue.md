# Despliegue en producción (2026-11-13)

Registro del despliegue de AulaSmart en la instancia Ubuntu de Oracle Cloud.
Separación de hechos verificados, decisiones y trabajo pendiente. Nada de este
documento sustituye la verificación con el código vigente.

- Fecha: 2026-11-13
- Rama de trabajo: `main`
- Referencia Git: `0573644` (frontend CI) / código desplegado: `6cda410` (commit de `main` al desplegar)

## Infraestructura

- VM Oracle Cloud: Ubuntu, 2 vCPU, 1 GB RAM (se agregó swap de 2 GB), 45 GB disco.
- IP pública: `150.136.80.177`
- Dominio: `stevendz.tech` y `www.stevendz.tech` (registros A en el panel del
  registrador, zona delegada a ClouDNS). Verificado con resolución a la IP.
- Usuario de acceso: `ubuntu` con llave SSH personal del propietario.

## Arquitectura desplegada

- **PostgreSQL 17.4** y **backend Spring Boot (perfil `prod`)** en Docker Compose
  (`compose.yaml`, servicios `db` y `backend`) desde el repo clonado en
  `/opt/aulasmart`. `restart: unless-stopped`; arrancan al reiniciar la VM.
- **Nginx en el host** (no en contenedor): sirve la SPA desde `/var/www/aulasmart`
  y hace proxy de `/api/` y `/actuator/health` a `127.0.0.1:8080`. Sitio en
  `/etc/nginx/sites-available/aulasmart`. Bloquea el resto de `/actuator/`.
- **HTTPS con Let's Encrypt** vía certbot (`--nginx`, redirección automática de
  HTTP a HTTPS). Certificado para `stevendz.tech` y `www`, vence 2027-02-11,
  renovación automática programada (`certbot renew --dry-run` aprobado).
- **CI/CD del frontend** (`.github/workflows/deploy-frontend.yml`): push a `main`
  con cambios en `aulasmart-frontend/**` (o `workflow_dispatch`) ejecuta pruebas
  (100, vitest con `--watch=false`), build de producción y rsync por SSH del
  `dist/aulasmart-frontend/browser/` a `/var/www/aulasmart/`. Verificado con un
  run real exitoso (run `43364235865`). Pin exacto de la action
  `easingthemes/ssh-deploy@v6.0.3` (el tag `@v5` no existe: causó tres runs
  fallidos en «Set up job»).

## Secretos y credenciales

- `/opt/aulasmart/.env` (permisos 600, solo en la VM): contraseñas de Postgres y
  backend (iguales, generadas con `openssl rand -hex 24`), `AULASMART_JWT_SECRET`
  (`openssl rand -base64 48`), `SPRING_PROFILES_ACTIVE=prod`,
  `AULASMART_CORS_ORIGINS=https://stevendz.tech`, `GROQ_API_KEY`,
  `AULASMART_AULABOT_ENABLED=true`, `AULASMART_ALLOW_TEST_ACCOUNTS=false`.
- `/opt/aulasmart/.credencial-admin` (permisos 600): cuenta real de
  administrador `admin@stevendz.tech` con contraseña aleatoria; cambiarla en el
  primer inicio de sesión. Las cuentas de prueba de V7 quedan deshabilitadas
  para inicio de sesión por `AULASMART_ALLOW_TEST_ACCOUNTS=false`.
- Secretos de GitHub Actions: `DEPLOY_HOST` (IP) y `DEPLOY_SSH_KEY` (llave
  ed25519 de despliegue, privada guardada junto a la llave personal del
  propietario; la pública está en `authorized_keys` de la VM). Configurados vía
  API de GitHub con cifrado sealed box; el token usado no se imprimió ni
  persistió (borrado tras el uso).
- La VM no tiene la contraseña de Postgres ni el JWT secret en ningún archivo
  versionado; `.env` está fuera de Git.

## Evidencia verificada en vivo

- Flyway: 11 migraciones aplicadas en la base nueva `aulasmart` (PostgreSQL 17.4).
- Login por `https://stevendz.tech/api/auth/login` (TLS de Nginx): HTTP 200 con
  JWT de acceso y cookie de refresh `Secure; HttpOnly; SameSite=Strict`.
- `GET /api/usuarios/whoami` con el JWT: 200, rol ADMINISTRADOR.
- `GET /api/aulas` sin token: 401 (protección correcta).
- `https://stevendz.tech/` y `/actuator/health`: 200 por HTTPS desde Internet.
- Sincronización inicial SIGA (bootstrap local, ver abajo): 106 aulas y 1.761
  registros de horario en producción; filas 9 y 10 de `sincronizacion_siga`
  EXITOSA.

## Limitación conocida: SIGA bloquea la IP de Oracle (hecho verificado)

- Desde la VM, `https://uceva.datasae.co/siga_new/web/app.php` responde **403
  Forbidden en <1 s** (también con User-Agent de navegador); desde la red
  doméstica del propietario responde 302 normal. Es un bloqueo de WAF por IP de
  datacenter: no es corregible desde este despliegue.
- Consecuencia: los ciclos programados de la VM (cada 15 min) fallan con 403;
  el catálogo cargado por bootstrap se considera vigente ~24 h
  (`aulasmart.disponibilidad.max-antiguedad=PT24H`). Después, la app muestra
  disponibilidad desconocida hasta una sincronización exitosa (estado diseñado,
  no un error).
- Opciones: (1) pedir a Datasae/la universidad el blanqueo de `150.136.80.177`,
  solución definitiva; (2) re-ejecutar el bootstrap por túnel SSH cuando se
  necesite refrescar el catálogo.

## Procedimiento de bootstrap SIGA (probado el 2026-11-13)

Objetivo: refrescar el catálogo de aulas y la ocupación desde una red no
bloqueada por SIGA (la doméstica), escribiendo en la base de producción.

1. Túnel SSH: `ssh -N -L 127.0.0.1:55433:127.0.0.1:5433 ubuntu@150.136.80.177`
   (el puerto 5433 del host está publicado solo en `127.0.0.1`).
2. Copiar `DB_PASSWORD`/`POSTGRES_USER` del `.env` del servidor a variables de
   entorno locales **sin imprimirlas** (script en el anexo del agente).
3. Ejecutar el jar del backend local (`mvnw.cmd -o clean package -DskipTests`)
   con:
   - `DB_URL=jdbc:postgresql://127.0.0.1:55433/aulasmart`
   - `SPRING_PROFILES_ACTIVE=local`
   - `AULASMART_AULABOT_ENABLED=false`
   - `AULASMART_SIGA_SYNC_CRON=0 0 4 1 1 *` (desactiva el ciclo programado local)
   - `SERVER_PORT=8081` (evita chocar con el backend local de desarrollo)
4. Esperar «Sincronización inicial AULAS completada» y «EVENTOS completada»
   (puede tardar ~10 min por las llamadas por aula).
5. Verificar en la base de producción: filas EXITOSA con registros en
   `sincronizacion_siga`, conteos en `aula` y `horario_academico`.
6. Detener el proceso local **solo después** de que la fila de EVENTOS esté
   EXITOSA (si se corta a mitad, la fila queda EN_CURSO y la vigencia no
   avanza), cerrar el túnel y borrar las credenciales temporales.

Nota: el arranque local puede regresar en silencio si considera el catálogo
vigente; verificar con los conteos de la base antes de asumir éxito.

## Pendientes

- Blanqueo de la IP de Oracle en el WAF de SIGA (gestión externa) o acuerdo de
  refresco manual del catálogo.
- Cambiar la contraseña del administrador tras el primer acceso.
- Clave Groq actual sigue expuesta según el registro de fase 5; reemplazarla
  cuando se rote (está en `/opt/aulasmart/.env` del servidor).
- Documentar en este archivo cualquier cambio de infraestructura futura
  (nueva VM A1, dominio adicional, certificado propio, etc.).
