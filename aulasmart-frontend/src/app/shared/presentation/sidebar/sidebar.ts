import { Component, OnInit, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth-service';
import { ACADEMIC_PERIOD, semanaDelPeriodo } from '../../../core/config/academic-period';
import { GET_CLASSROOM_REQUESTS_USE_CASE } from '../../../features/solicitudes/application/classroom-requests-di';
import { GetClassroomRequestsUseCase } from '../../../features/solicitudes/application/use-cases/get-classroom-requests.use-case';
import { ShellState } from '../shell-state';

/** Enlace lateral con su ícono (nombre del trazado SVG asociado). */
interface SidebarLink {
  path: string;
  label: string;
  icon: 'dashboard' | 'consulta' | 'aulabot' | 'solicitudes' | 'aulas' | 'usuarios';
  /** Muestra el conteo de solicitudes pendientes (dato real de la API). */
  badgePendientes?: boolean;
}

/** Grupo de navegación del menú lateral (ej.: General, Administración). */
interface SidebarGroup {
  title: string;
  links: SidebarLink[];
}

/** Trazados de los íconos (línea, 24×24) usados por la barra lateral. */
const ICONOS: Record<SidebarLink['icon'] | 'marca' | 'bloques', { d: string; cerrar?: boolean }> = {
  dashboard: { d: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z', cerrar: true },
  consulta: { d: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16.2 16.2 21 21' },
  aulabot: {
    d: 'M12 3l1.9 4.9L18.8 9.8l-4.9 1.9L12 16.6l-1.9-4.9L5.2 9.8l4.9-1.9zM18.5 15.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z',
    cerrar: true
  },
  solicitudes: { d: 'M6 3h8l4 4v14H6zM9 11h6M9 15h6' },
  aulas: { d: 'M4 21V7l8-3v17M12 21h8v-9l-8-2.5M7 11h2M7 15h2M15 12h2M15 16h2', cerrar: true },
  usuarios: {
    d: 'M9 4.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7zM2.5 20c0-3.6 2.9-5.5 6.5-5.5s6.5 1.9 6.5 5.5M16.5 5.5a2.8 2.8 0 1 1 0 5.6M17.5 14.7c2.3.7 4 2.3 4 5.3',
    cerrar: false
  },
  marca: { d: 'M12 4 22 9.2l-10 5.2L2 9.2zM6.5 12v4.5c0 1.6 2.5 3 5.5 3s5.5-1.4 5.5-3V12', cerrar: false },
  bloques: { d: 'M4 6h16M10 3v6M4 12h16M4 18h16', cerrar: true }
};

/**
 * Menú lateral del shell (diseño aprobado): fondo blanco, íconos por ítem,
 * grupos General / Administración filtrados por el rol real de la sesión,
 * badge con las solicitudes pendientes (API real) y tarjeta de período
 * académico con avance de semana.
 */
@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  styleUrl: './sidebar.css',
  template: `
    <aside class="sidebar" [class.colapsada]="shell.colapsada()">
      <div class="marca">
        <span class="logotipo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path [attr.d]="ICONOS.marca.d" />
          </svg>
        </span>
        <span class="nombres">
          <span class="titulo">AulaSmart</span>
          <span class="subtitulo">UCEVA · Tuluá</span>
        </span>
      </div>

      @for (grupo of grupos(); track grupo.title) {
        <nav class="grupo">
          @if (!shell.colapsada()) {
            <span class="titulo-grupo">{{ grupo.title }}</span>
          }
          @for (enlace of grupo.links; track enlace.path) {
            <a
              [routerLink]="enlace.path"
              routerLinkActive="activo"
              [routerLinkActiveOptions]="{ exact: enlace.path === '/' }"
              [title]="enlace.label"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path [attr.d]="ICONOS[enlace.icon].d" />
              </svg>
              <span class="etiqueta">{{ enlace.label }}</span>
              @if (enlace.badgePendientes && (pendientes() ?? 0) > 0) {
                <span class="insignia">{{ pendientes() }}</span>
              }
            </a>
          }
        </nav>
      }

      <div class="periodo">
        <span class="titulo">Período {{ periodo.label }}</span>
        @if (semana !== null) {
          <span class="detalle">Semana {{ semana }} de {{ periodo.totalWeeks }}</span>
          <div class="avance">
            <span class="relleno" [style.width.%]="(semana / periodo.totalWeeks) * 100"></span>
          </div>
        } @else {
          <span class="detalle">{{ periodo.totalWeeks }} semanas · fuera de rango</span>
        }
      </div>
    </aside>
  `
})
export class Sidebar implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly getRequests = inject<GetClassroomRequestsUseCase>(GET_CLASSROOM_REQUESTS_USE_CASE);
  protected readonly shell = inject(ShellState);

  protected readonly periodo = ACADEMIC_PERIOD;
  protected readonly ICONOS = ICONOS;

  /** Semana vigente fija en la creación de la sesión de navegación. */
  protected readonly semana: number | null = semanaDelPeriodo(ACADEMIC_PERIOD, new Date());

  /** Conteo real de solicitudes pendientes visibles para el rol (null sin dato). */
  protected readonly pendientes = signal<number | null>(null);

  /**
   * Grupos visibles según la sesión: «Administración» solo para
   * ADMINISTRATIVO / ADMINISTRADOR (decisión documentada alineada con la
   * SecurityConfiguration real del backend).
   */
  protected readonly grupos = computed<SidebarGroup[]>(() => {
    const grupos: SidebarGroup[] = [
      {
        title: 'General',
        links: [
          { path: '/', label: 'Dashboard', icon: 'dashboard' },
          { path: '/consulta', label: 'Consulta de aulas', icon: 'consulta' },
          { path: '/aulabot', label: 'AulaBot', icon: 'aulabot' },
          { path: '/solicitudes', label: 'Solicitudes', icon: 'solicitudes', badgePendientes: true }
        ]
      }
    ];

    if (this.authService.tieneAlgunRol(['ADMINISTRATIVO', 'ADMINISTRADOR'])) {
      grupos.push({
        title: 'Administración',
        links: [
          { path: '/aulas', label: 'Gestión de aulas', icon: 'aulas' },
          { path: '/usuarios', label: 'Usuarios', icon: 'usuarios' }
        ]
      });
    }

    return grupos;
  });

  ngOnInit(): void {
    if (this.authService.sesion() === null) {
      return;
    }
    this.getRequests.execute('PENDIENTE').subscribe({
      next: solicitudes => this.pendientes.set(solicitudes.length),
      error: () => this.pendientes.set(null)
    });
  }
}
