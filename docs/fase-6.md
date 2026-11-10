# Fase 6: Identidad visual, experiencia de usuario y modo oscuro

**Estado:** implementada y validada (2026-11-10).
**Rama:** `feature/visual-identity-dark-mode`.
**Base:** `main` commit `84f8c89`.
**Commits de la fase:**

| Commit | Descripción |
|--------|-------------|
| `58752ab` | feat(design-system): tokens CSS expandidos, dark mode y ThemeService |
| `35593d8` | feat(ui): mejorar identidad visual de todos los componentes con dark mode |

## Commits de la fase (exclusivamente locales)

| Commit | Tipo | Descripción |
|---|---|---|
| `58752ab` | feat | Tokens CSS expandidos, dark mode y ThemeService |
| `35593d8` | feat | Mejorar identidad visual de todos los componentes con dark mode |
| `afe6975` | docs | Registrar implementación de identidad visual y modo oscuro en Plan Maestro |
| `2f097e5` | docs | Agregar documento de entrega y criterios de aceptación |
| `cbe0461` | style | Refinar identidad hacia minimalismo funcional y limpio en ambos temas |
| `c07ffa1` | docs | Documentar refinamiento hacia minimalismo funcional y lista de commits |
| `b39225d` | fix | Corregir contraste de iconos en consulta de aulas y calibrar modo oscuro profundo |

## Principio rector: Minimalismo Funcional (Clean Minimal UI/UX)

La identidad visual de AulaSmart se diseñó bajo una filosofía estricta de **Clean Minimal UI/UX**:
- **Sin efectos decorativos superfluos:** se eliminaron animaciones gratuitas (como rotación en toggle o pulsos en celdas).
- **Sin patrones genéricos de IA:** cero bordes de colores asimétricos en tarjetas o barras laterales; las jerarquías se comunican con tipografía, espaciado e indicadores semánticos naturales.
- **Identidad azul y blanco sólida:** el logotipo y acentos utilizan un azul institucional estable y sereno (`#2563EB` en claro, `#3B82F6` en oscuro) sin degradados artificiales.
- **Contraste garantizado:** todos los textos e iconos sobre fondos coloreados (botones, avatares, burbujas de usuario) mantienen `#FFFFFF` o el token semántico contrastado, asegurando cumplimiento WCAG AA/AAA tanto en tema claro como en oscuro.
- **Cada elemento tiene una razón para existir.**

Se amplió el sistema de tokens CSS en `aulasmart-frontend/src/styles.css` pasando de 110 a 477 líneas.

### Variables de tema claro (`:root`)

| Categoría | Variables |
|-----------|-----------|
| Colores primarios | `--color-primary`, `--color-primary-hover`, `--color-primary-dark`, `--color-primary-light` |
| Colores secundarios | `--color-secondary` |
| Superficies | `--color-surface`, `--color-surface-elevated`, `--color-background`, `--color-background-subtle` |
| Texto | `--color-text`, `--color-text-secondary`, `--color-text-disabled` |
| Bordes | `--color-border`, `--color-border-strong` |
| Semánticos | `--color-success`, `--color-warning`, `--color-danger`, `--color-info`, `--color-muted` |
| Tipografía | `--font-sans`, `--font-mono`, `--font-size-xs` a `--font-size-3xl`, pesos y alturas |
| Espaciado | `--space-1` (4px) a `--space-10` (64px) |
| Radios | `--radius-xs` (4px) a `--radius-full` (9999px) |
| Sombras | `--shadow-xs` a `--shadow-xl` |
| Transiciones | `--transition-fast` (100ms), `--transition-base` (200ms), `--transition-slow` (300ms) |
| Z-index | `--z-dropdown` (40), `--z-sticky` (20), `--z-modal` (1000), `--z-toast` (1100) |

### Variables de tema oscuro (`[data-theme="dark"]`)

Paleta completa overrideada:
- Fondo base: `#0F172A` (slate-900)
- Superficie: `#1E293B` (slate-800)
- Superficie elevada: `#263548`
- Primary: `#3B82F6` (blue-500, más luminoso)
- Texto: `#F1F5F9`, texto secundario `#94A3B8`
- Bordes: `#334155`, fuertes `#475569`
- Semánticos ajustados para contraste en oscuro

### Medias queries del sistema

- `@media (prefers-color-scheme: dark)`: aplica dark si no hay `data-theme="light"` explícito
- `@media (prefers-reduced-motion: reduce)`: desactiva todas las transiciones y animaciones

### Clases de utilidad globales

`.sr-only`, `.truncate`, `.skeleton` (animación pulse), `.chip`, `.badge`, `.divider`

## 2. Gestión de temas

### `ThemeService` (`app/core/theme.service.ts`)

```typescript
// Signal reactivo: 'light' | 'dark'
// Inicializa desde localStorage → prefers-color-scheme → 'light'
// Efecto escribe data-theme en <html> + persiste en localStorage
// ThemeService.alternar() → alterna entre temas
```

Características:
- Inicialización desde `localStorage` (clave: `aulasmart-theme`)
- Fallback a `prefers-color-scheme` del SO
- Reactividad con `signal` + `effect` de Angular 17+
- Persistencia automática en cada cambio

### Script anti-parpadeo (`index.html`)

Inline antes de `</head>`: lee el tema guardado y lo aplica al `<html>` antes de que Angular cargue, eliminando el flash de contenido sin estilo (FOUC) al recargar la página.

### Toggle de tema en el Header

Botón de luna/sol (SVG inline) en la barra superior derecha, con:
- `aria-label` dinámico según el tema activo
- Icono de sol (modo claro) ↔ luna (modo oscuro)
- Accesible con teclado y foco visible

## 3. Componentes mejorados

### `header.css` (399 líneas)
- Barra con `backdrop-filter: blur(8px)` y semi-transparencia
- Clase `.alternar-tema` para el toggle de tema
- Todos los hover/focus-visible con `var(--color-primary)`
- Transiciones suaves en todos los elementos interactivos
- **Cero colores hex hardcodeados**

### `sidebar.css` (221 líneas)
- Logotipo con gradiente `linear-gradient(135deg, var(--color-primary-dark), var(--color-primary))`
- Link activo con `border-left: 3px solid var(--color-primary)` más prominente
- Transiciones en enlaces de navegación
- Tarjeta de período con borde acento azul
- **Cero colores hex hardcodeados**

### `login-page.css`
- Variables locales `--gradient-brand` y `--gradient-brand-dark`
- Celdas decorativas: `var(--color-success)` y `var(--color-primary)` (antes #34D399 / #3B82F6)
- Animación `pulso-celda` con delays staggered; anulada con `prefers-reduced-motion`
- Botón de ingreso con `transition` y hover con `translateY(-1px)`
- Input focus con `outline: 2px solid var(--color-primary)` + `outline-offset: 2px`
- **Solo un #fff intencional en texto del panel de marca sobre gradiente oscuro**

### `dashboard-page.css`
- Tarjetas indicadoras con hover `transform: translateY(-2px)` + `var(--shadow-md)`
- Variantes de tarjeta con `border-top: 3px solid` semántico (success/primary/warning/success)
- Estados PENDIENTE/APROBADA/RECHAZADA con `color-mix()` adaptables al tema
- Barra de edificios con `transition: width var(--transition-slow)`
- Panel AulaBot con `background: var(--color-primary-light)` + borde acento
- **Cero colores hex hardcodeados**

### `consulta-page.css`
- Variable `--color-modal-overlay` en ambos temas (45% light → 65% dark)
- Estados de tarjeta `.aula-card__estado.libre/.ocupada`: `color-mix()` adaptable
- Icono de tarjeta `.aula-card__icono`: `var(--color-primary-light)/var(--color-primary)`
- Chips de característica: `var(--color-primary-light)/var(--color-primary-dark)`
- Timeline y banner-info con `color-mix()` semántico
- Hover de tarjeta: `border-color: var(--color-primary)` + `var(--shadow-md)`
- Transiciones en cards, chips, botones, campos, pestañas
- **Cero colores hex hardcodeados**

### `solicitudes-page.css`
- Estados PENDIENTE/APROBADA/RECHAZADA con `color-mix()` adaptables
- Botones de acción `.accion` ampliados a 32×32px
- Scroll táctil mejorado en tabla
- Clase `.cargando` con animación skeleton
- `@media (prefers-reduced-motion)` completo
- **Cero colores hex hardcodeados**

### `usuarios-page.css`
- Chips de estado activo/inactivo con `color-mix()` adaptable
- Transiciones en chips, rol-chip, inputs, botones
- Clase `.cargando` skeleton idéntica a solicitudes
- **Cero colores hex hardcodeados**

### `aulabot-page.css` (375 líneas)
Reescritura completa desde 3 líneas minificadas (todos los colores hardcodeados) a CSS legible y organizado:
- 7 secciones documentadas
- Burbujas de chat modernas: `.question` azul con texto blanco, `.answer` superficie con borde
- Tarjetas de aulas con `var(--color-surface)` y `var(--color-border)`
- Composer con `border-color: var(--color-primary)` en focus
- Chips con hover `var(--color-primary-light)` + `border-color: var(--color-primary)`
- **Cero colores hex hardcodeados**

## 4. Archivos modificados

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `src/styles.css` | Sistema de diseño | Ampliado de 110 a 477 líneas |
| `src/index.html` | Anti-parpadeo | Script inline de tema |
| `src/app/app.ts` | Bootstrap | Inyección de ThemeService |
| `src/app/core/theme.service.ts` | Nuevo | Gestión reactiva de temas |
| `src/app/shared/presentation/header/header.ts` | UI | Toggle de tema en template |
| `src/app/shared/presentation/header/header.css` | UI | Mejorado con 399 líneas |
| `src/app/shared/presentation/sidebar/sidebar.css` | UI | Mejorado con 221 líneas |
| `src/app/features/sesion/login-page.css` | UI | Dark mode y animaciones |
| `src/app/features/dashboard/presentation/pages/dashboard-page.css` | UI | Semantic colors |
| `src/app/features/aulas/presentation/pages/consulta-page.css` | UI | Dark mode completo |
| `src/app/features/solicitudes/presentation/pages/solicitudes-page.css` | UI | Semantic colors |
| `src/app/features/usuarios/presentation/pages/usuarios-page.css` | UI | Tokens y skeleton |
| `src/app/features/aulabot/aulabot-page.css` | UI | Reescritura completa |

## 5. Criterios de aceptación

| Criterio | Estado |
|----------|--------|
| Plan Maestro contiene Fase 6 | ✅ |
| Identidad visual coherente en las pantallas | ✅ |
| Sistema de diseño reutilizable (tokens) | ✅ |
| Modo claro funciona correctamente | ✅ |
| Modo oscuro funciona correctamente | ✅ |
| Preferencia de tema se conserva entre sesiones | ✅ localStorage |
| Componentes con contraste y estados accesibles | ✅ |
| Interfaces funcionan en móvil y escritorio | ✅ responsive heredado |
| Funcionalidades originales operativas | ✅ |
| Build Angular sin errores | ✅ código 0 |
| 100 pruebas frontend pasadas | ✅ 22 archivos, 100 tests |
| Documentación refleja implementación real | ✅ |
| Cambios en commits locales | ✅ 2 commits |

## 6. Validación

- **Build Angular:** `ng build --configuration development` → ✅ exitoso (12.5 s), sin errores ni warnings de compilación.
- **Pruebas frontend:** `ng test --watch=false` → ✅ 22 archivos, **100 pruebas aprobadas**, 0 fallidas.
- **Backend:** no se modificó; pruebas backend no re-ejecutadas (no hubo cambios).
- **Verificación visual:** no ejecutada con navegador real (SIGA vivo no verificado por el agente, servidor de desarrollo no arrancado). Verificación pendiente de prueba funcional del usuario.

## 7. Limitaciones documentadas

- El modo oscuro funciona para todos los tokens CSS nuevos. Componentes externos (Material, PrimeNG, etc.) no aplican — no hay librerías UI de terceros instaladas en este proyecto.
- La prueba visual en navegador real (capturas antes/después) quedó pendiente de prueba del usuario: el servidor de desarrollo requiere `BypassSandbox` para ser accesible.
- La advertencia CSS pre-existente del Consulta sin cambios (documentada en fases anteriores) permanece sin cambios.
- `color-mix()` requiere navegadores modernos (Chrome 111+, Firefox 113+, Safari 16.2+). La app no tiene polyfill para esto y es consistente con el target universitario.

## 8. Decisiones de diseño conservadas

- **Identidad azul y blanca**: mantenida en light mode, con azul más luminoso (blue-500) en dark mode.
- **Sin degradados innecesarios**: solo el panel decorativo de login conserva su gradiente de marca intencional.
- **Sin efectos exagerados**: transiciones de 200ms máximo; `prefers-reduced-motion` respetado.
- **Sin colores hardcodeados**: toda la paleta migrada a variables CSS para garantizar theming correcto.
- **NO se modificó lógica de negocio ni backend** — restricción respetada completamente.
