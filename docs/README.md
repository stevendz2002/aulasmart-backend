# Documentación técnica de continuidad

## Migración de AulaSmart a GitHub Pages (2026-11-14)

Rama `refactor/github-pages-frontend`, base `88fa573`. El flujo anterior de SSH está descrito en el [registro del despliegue de Oracle](despliegue.md).

El acceso previsto es [stevendz.tech/aulasmart/](https://stevendz.tech/aulasmart/). Nginx en Oracle sirve esa ruta mediante un proxy HTTPS al artefacto público de Pages en `stevendz2002.github.io/aulasmart-backend/`. La raíz `/` queda disponible para el portafolio. El DNS y los certificados del dominio pueden seguir apuntando a Oracle.

El workflow `deploy-frontend` se ejecuta al cambiar el frontend o el workflow en `main`, y admite ejecución manual desde esa rama. Usa Node 22, `npm ci` y Vitest, compila `production,pages` y publica `dist/aulasmart-frontend/browser/` en el entorno `github-pages`. La base relativa `./` resuelve los recursos bajo ambos prefijos y Angular usa rutas con fragmento, como `/aulasmart/#/consulta`. El resultado de Actions enlaza el origen de Pages. El acceso con sesión se hace desde `stevendz.tech`.

El navegador llama a `/api` en su propio origen y Nginx dirige esas peticiones a Spring Boot en Oracle. Esto conserva JWT Bearer, la cookie HttpOnly `SameSite=Strict` y la verificación de origen y CSRF. El proxy de archivos descarta las cabeceras entrantes, incluidas cookies y autorización, antes de solicitar recursos a GitHub.

Con Node 22 pasaron las 100 pruebas de Vitest y el build de Pages. `actionlint` validó el workflow. En Edge se verificaron JavaScript, CSS, favicon, navegación y recarga bajo `/aulasmart/` y `/aulasmart-backend/`, con la API simulada en el mismo origen. Queda pendiente validar Nginx y la sesión contra Oracle en la ruta pública.

1. En **Settings → Pages → Build and deployment → Source**, seleccionar **GitHub Actions** y dejar el dominio de Pages predeterminado. El entorno `github-pages` debe permitir `main`. Tras integrar los cambios revisar el workflow o lanzarlo manualmente desde **Actions → deploy-frontend**.
2. Comprobar que el origen de Pages devuelve el `index.html` con `<base href="./">`. Instalar [el fragmento del proxy](../deploy/nginx/aulasmart-pages.conf) en la VM e incluirlo dentro del bloque HTTPS de `stevendz.tech` en `/etc/nginx/sites-available/aulasmart`. Mantener las rutas existentes de `/api/` y `/actuator/health`. Validar con `sudo nginx -t` antes de recargar.
3. Comprobar el redireccionamiento de `/aulasmart`, la carga de recursos en `/aulasmart/`, la API y la sesión tras recargar una ruta. Una vez comprobada la ruta nueva, retirar la regla que sirve la SPA antigua en `/` o reemplazarla por la configuración del portafolio.

Estado vigente (2026-11-10): AulaBot publicado en main `1e7da77`. Corrección de solicitudes
aceptada en `feature/solicitudes-historial`, publicación en main autorizada. Las secciones anteriores de fase 5
describen su evolución; prevalece la actualización final de este documento.


Última revisión: 2026-11-09. Fase 4 implementada en `6965a7a`; cierre aprobado.
Ruta de entrega autorizada: `feature/jwt-authentication` → `develop` → `main`.

1. Leer [plan maestro](plan-maestro.md) para alcance y autorizaciones.
2. Consultar [arquitectura](arquitectura.md) para ubicar los módulos relevantes.
3. Leer [fase 0](fase-0.md) para cambios, validaciones y pendientes.
   Consultar la implementación y validación de [fase 2](fase-2.md).
4. Contrastar lo necesario con Git y el código antes de trabajar.

La fase 0 está publicada en `origin/refactor/architecture`, commit `7030f16`.
La fase 2 está publicada en `origin/feature/single-user-role`, commit `621e3d6`.
Fase 1 publicada en `origin/feature/classroom-request`: `7c5779f`.
Tarjetas aprobadas y publicadas: `f481bad` en `origin/refactor/classroom-search-ui`.
Búsquedas aprobadas y publicadas: `5fea5b4`.
[Corrección SIGA](sincronizacion-siga.md) publicada: `f432436`.
[Fase 4: autenticación y configuración](fase-4.md) aceptada por el usuario,
con publicación en `develop` y después `main` autorizada.
Fase 4 publicada en ambas ramas: `5e4b4da`.
Trabajo actual: [fase 5, AulaBot](fase-5.md), implementación local autorizada en
`feature/aulabot`, sobre `5e4b4da`. Publicación pendiente de aprobación del usuario.
Revisión 5A–5E implementada localmente el 2026-11-10 (memoria en servidor,
equipamiento, alternativas sin resultados y chat único), también sin commit;
ver `fase-5.md`, sección «Revisión autorizada 5A–5E».
No sustituyen la inspección de diferencias ni demuestran que una base instalada
ya recibió las migraciones. El informe de auditoría previo mencionado por el
usuario no fue localizado; esta documentación registra la revisión actual.

Los archivos académicos preexistentes siguen siendo locales e ignorados por Git.
Solo los documentos técnicos indicados explícitamente en `.gitignore` están habilitados para versionarse.
`AGENTS.md` contiene las reglas permanentes de trabajo.

## Actualizaciones

Al cerrar una fase: registrar fecha, referencia Git, alcance implementado,
decisiones, pruebas con resultado real, limitaciones y próximos pasos autorizados.
No repetir listados completos ni copiar reportes generados; enlazar archivos fuente
y resumir evidencia verificable. Si el código contradice un documento, corregir
el documento y dejar constancia de la discrepancia.

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
