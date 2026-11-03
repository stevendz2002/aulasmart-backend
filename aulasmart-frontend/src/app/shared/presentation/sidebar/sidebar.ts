import { Component, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth-service';

/** Enlace del menú lateral. */
interface SidebarLink {
  path: string;
  label: string;
  /** Letra mostrada cuando la barra está colapsada. */
  initial: string;
}

/** Grupo de navegación del menú lateral (ej.: General, Administración). */
interface SidebarGroup {
  title: string;
  links: SidebarLink[];
}

/** Umbral en el que la barra inicia colapsada (pantallas angostas). */
const UMBRAL_COLAPSO_PX = 720;

/** Verdadero en ventanas angostas con soporte de matchMedia (p. ej. navegador). */
function iniciaColapsada(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia(`(max-width: ${UMBRAL_COLAPSO_PX}px)`).matches
  );
}

/**
 * Menú lateral del shell: navegación agrupada (General / Administración) y
 * filtrada por el rol real de la sesión. Colapsable a iniciales de un ancho
 * reducido, con la identidad azul de la spec (§11).
 */
@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  styleUrl: './sidebar.css',
  template: `
    <aside class="sidebar" [class.colapsada]="colapsada()">
      <div class="marca">
        @if (colapsada()) {
          <a routerLink="/" [attr.title]="'AulaSmart'" class="sigla">A</a>
        } @else {
          <a routerLink="/">AulaSmart</a>
        }
        <button
          type="button"
          class="alternar"
          (click)="alternar()"
          [attr.aria-label]="colapsada() ? 'Expandir menú' : 'Colapsar menú'"
        >
          {{ colapsada() ? '»' : '«' }}
        </button>
      </div>

      @for (grupo of grupos(); track grupo.title) {
        <nav class="grupo">
          @if (!colapsada()) {
            <span class="titulo">{{ grupo.title }}</span>
          }
          @for (enlace of grupo.links; track enlace.path) {
            <a
              [routerLink]="enlace.path"
              routerLinkActive="activo"
              [routerLinkActiveOptions]="{ exact: enlace.path === '/' }"
              [title]="enlace.label"
            >
              {{ colapsada() ? enlace.initial : enlace.label }}
            </a>
          }
        </nav>
      }
    </aside>
  `
})
export class Sidebar {
  private readonly authService = inject(AuthService);

  protected readonly colapsada = signal(iniciaColapsada());

  /**
   * Grupos visibles según la sesión: «Administración» solo para
   * ADMINISTRATIVO / ADMINISTRADOR (decisión documentada: el backend deja la
   * gestión en manos del personal administrativo).
   */
  protected readonly grupos = computed<SidebarGroup[]>(() => {
    const grupos: SidebarGroup[] = [
      {
        title: 'General',
        links: [
          { path: '/', label: 'Dashboard', initial: 'D' },
          { path: '/consulta', label: 'Consulta', initial: 'C' },
          { path: '/aulabot', label: 'AulaBot', initial: 'B' },
          { path: '/solicitudes', label: 'Solicitudes', initial: 'S' }
        ]
      }
    ];

    if (this.authService.tieneAlgunRol(['ADMINISTRATIVO', 'ADMINISTRADOR'])) {
      grupos.push({
        title: 'Administración',
        links: [
          { path: '/aulas', label: 'Gestión de aulas', initial: 'G' },
          { path: '/usuarios', label: 'Usuarios', initial: 'U' }
        ]
      });
    }

    return grupos;
  });

  protected alternar(): void {
    this.colapsada.update(colapsada => !colapsada);
  }
}
