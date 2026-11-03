# AulaSmart Frontend (SPA Angular)

Este proyecto es la SPA del sistema **AulaSmart** de la UCEVA: gestión y consulta de reservas de aulas sincronizadas desde SIGA. El **backend monolítico modular** (`aulasmart-backend/`) es la única fuente de verdad: el frontend consume únicamente los endpoints que ese backend publica, sin datos simulados ni endpoints inventados.

---

## Estructura (Clean Architecture)

```
src/app/
├── core/                  Sesion (HTTP Basic), interceptor, guardas de ruta,
│                          url de API y configuración del período académico.
├── shared/presentation/   Shell: sidebar agrupado por roles, header con sesión
│                          real y footer con período académico (2026-2).
└── features/
    aulas/                 Catálogo y disponibilidad + sincronización SIGA.
    solicitudes/           Ciclo de vida de solicitudes de reserva.
    dashboard/             Indicadores en vivo.
    usuarios/              Perfil de sesión (endpoint whoami).
    aulabot/               Lugar honesto: depende de módulo chatbot pendiente.
    sesion/                Inicio de sesión.
```

Cada feature se organiza en 4 capas:

| Capa | Carpeta | Regla |
|---|---|---|
| **Dominio** | `domain/entities`, `domain/repositories` | TypeScript puro: sin Angular, sin HttpClient. Define entidades y contratos (interfaces de repositorio). |
| **Aplicación** | `application/use-cases` | Casos de uso (`GetAllClassroomsUseCase`, …) que dependen solo de los contratos; testeables sin Angular. |
| **Infraestructura** | `infrastructure/{dto,mappers,repositories}` | Única capa que conoce JSON y URLs: DTOs en español tal como los expone el backend, mapeadores (Adapter) hacia el dominio y repositorios HTTP. |
| **Presentación** | `presentation/pages` | Componentes standalone con signals; consumen casos de uso inyectados, nunca HttpClient. |

El cableado (Dependency Inversion) queda en `infrastructure/*-providers.ts`, registrado en `app.config.ts` con tokens de DI de `application/*-di.ts`.

## Convenciones

- Identificadores en **inglés** (camelCase / PascalCase); textos de UI en español colombiano.
- Archivos en kebab-case con sufijos por rol: `.use-case.ts`, `.repository.ts`, `-page.ts`.
- Tokens de diseño centralizados en `src/styles.css` como variables CSS
  (identidad azul y blanco: `--color-primary: #2563EB`, `--color-primary-dark: #123B70`, …).

## Rutas y roles

El guard `requiereRoles()` exige sesión (HTTP Basic) y, opcionalmente, roles. La matriz sigue la seguridad real del backend:

| Ruta | Vista | Roles (backend) |
|---|---|---|
| `/` | Dashboard (KPIs en vivo) | cualquier sesión |
| `/consulta` | Catálogo y disponibilidad `[inicio, fin)` | cualquier sesión |
| `/aulabot` | AulaBot | cualquier sesión |
| `/solicitudes` | Solicitudes (ver/crear/decidir según rol) | DOCENTE, ADMINISTRATIVO, ADMINISTRADOR |
| `/aulas` | Gestión de aulas + sincronizar SIGA | ADMINISTRATIVO, ADMINISTRADOR |
| `/usuarios` | Perfil de sesión | ADMINISTRATIVO, ADMINISTRADOR |
| `/login` | Inicio de sesión | — |

Ciclo de disponibilidad: `GET /api/aulas/disponibles` devuelve **503** cuando la sincronización SIGA supera su ventana de vigencia; el personal administrativo la relanza desde *Gestión de aulas* (`POST /api/sincronizaciones?recurso=AULAS`), y el indicador vuelve a mostrarse con datos reales.

## Puesta en marcha

Requiere el backend en `localhost:8080` (ver README raíz: Docker en `5433`, migraciones Flyway, base con datos de prueba `database/seeds/001_datos_prueba.sql`).

```bash
npm install
npm start          # Angular en http://localhost:4200
npm test           # vitest: dominio, repositorios HTTP y componentes del shell
npm run build      # build de producción
```

Credenciales del entorno de prueba (semilla sintética, no institucional):
`docente@prueba.aulasmart.local` / `revisor@prueba.aulasmart.local`, contraseña `password` (hash público de ejemplo). Autenticación por HTTP Basic en cada petición — la SPA no mantiene sesión en el servidor.

## Dependencias documentadas (alcance real)

El frontend **no simula** capacidades que el backend aún no publica; los lugares quedan explícitos en la UI:

- CRUD de aulas: solo lectura; escritura pendiente de endpoints del backend.
- Directorio de usuarios: solo `GET /api/usuarios/whoami`; listado completo pendiente.
- AulaBot: interfaz detallada pendiente de `use-case`/endpoint del módulo chatbot.
- Períodos académicos: configuración centralizada en `core/config/academic-period.ts` hasta publicar el endpoint correspondiente.
- Roles: la matriz anterior refleja la `SecurityConfiguration` real del backend (p. ej. crear solicitudes es exclusivo de DOCENTE y revisar pertenece a ADMINISTRATIVO/ADMINISTRADOR).
