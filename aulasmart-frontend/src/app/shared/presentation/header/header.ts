import {
  HostListener,
  Component,
  ChangeDetectionStrategy,
  computed,
  inject,
  signal
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth-service';
import { inicialesDesdeCorreo, nombreDesdeCorreo } from '../../../core/nombre-usuario';
import { GET_ALL_CLASSROOMS_USE_CASE } from '../../../features/aulas/application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../../features/aulas/application/use-cases/get-all-classrooms.use-case';
import { Classroom } from '../../../features/aulas/domain/entities/classroom';
import { ShellState } from '../shell-state';

/**
 * Barra superior del shell (diseño aprobado): botón de colapso, buscador de
 * aulas sobre el catálogo real (con atajo Ctrl/⌘K) e identidad de la sesión
 * con iniciales derivadas del correo y el rol real entregado por la API.
 * No hay campana de notificaciones: el backend no publica un endpoint de
 * notificaciones y no se simula.
 */
@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  styleUrl: './header.css',
  template: `
    <header class="barra">
      <button
        type="button"
        class="alternar"
        (click)="shell.alternarColapso()"
        aria-label="Colapsar o expandir menú"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M4 5h16v14H4zM9.5 5v14" />
        </svg>
      </button>

      <div class="buscador" [class.abierta]="resultados().length > 0">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16.2 16.2 21 21" />
        </svg>
        <input
          #entradaBusqueda
          type="search"
          placeholder="Buscar aula…"
          [value]="consulta()"
          (input)="consultar(entradaBusqueda.value)"
          (keydown.enter)="irAConsulta()"
          (keydown.escape)="limpiar()"
          aria-label="Buscar aula en el catálogo"
        />
        <span class="atajo">{{ atajoTexto }}</span>

        @if (consulta() !== '' && consultando()) {
          <div class="menu-resultados">
            <span class="fila apagada">Consultando catálogo…</span>
          </div>
        } @else if (resultados().length > 0) {
          <div class="menu-resultados">
            @for (aula of resultados(); track aula.id) {
              <button type="button" class="fila" (click)="irAConsulta()">
                <strong>{{ aula.name }}</strong>
                <span>{{ aula.building.name }} · capacidad {{ aula.capacity }}</span>
              </button>
            }
            <button type="button" class="fila" (click)="irAConsulta()">
              <span class="apagada">Ver «{{ consulta() }}» en la consulta de aulas →</span>
            </button>
          </div>
        }
      </div>

      @if (authService.sesion(); as sesion) {
        <div class="identidad">
          <button type="button" class="persona" (click)="alternarMenu()">
            <span class="avatar">{{ iniciales() }}</span>
            <span class="datos">
              <span class="nombre">{{ nombre() }}</span>
              <span class="rol">{{ sesion.roles.join(' · ') }}</span>
            </span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m7 10 5 5 5-5" />
            </svg>
          </button>
          @if (menuAbierto()) {
            <div class="menu-sesion">
              <span class="fila apagada" [attr.title]="sesion.correo">{{ sesion.correo }}</span>
              <button type="button" class="fila" (click)="cerrarSesion()">Cerrar sesión</button>
            </div>
          }
        </div>
      } @else {
        <a routerLink="/login" class="ingresar">Ingresar</a>
      }
    </header>
  `
})
export class Header {
  protected readonly authService = inject(AuthService);
  protected readonly router = inject(Router);
  protected readonly shell = inject(ShellState);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);

  protected readonly atajoTexto = typeof navigator !== 'undefined' && /Mac/i.test(navigator.platform ?? '') ? '⌘K' : 'Ctrl K';
  protected readonly consulta = signal('');
  protected readonly consultando = signal(false);
  protected readonly catalogo = signal<Classroom[] | null>(null);
  protected readonly menuAbierto = signal(false);

  protected readonly nombre = computed(() => {
    const sesion = this.authService.sesion();
    return sesion === null ? '' : nombreDesdeCorreo(sesion.correo);
  });

  protected readonly iniciales = computed(() => {
    const sesion = this.authService.sesion();
    return sesion === null ? '' : inicialesDesdeCorreo(sesion.correo);
  });

  protected readonly resultados = computed<Classroom[]>(() => {
    const catalogo = this.catalogo();
    const consulta = this.consulta().trim().toLowerCase();
    if (catalogo === null || consulta === '') {
      return [];
    }
    return catalogo.filter(aula => aula.name.toLowerCase().includes(consulta)).slice(0, 6);
  });

  @HostListener('document:keydown', ['$event'])
  enTecladoGlobal(evento: KeyboardEvent): void {
    if (this.esAtajoK(evento)) {
      evento.preventDefault();
      document.querySelector<HTMLInputElement>('app-header input[type=search]')?.focus();
    }
  }

  protected consultar(texto: string): void {
    this.consulta.set(texto);
    if (this.catalogo() !== null) {
      return;
    }
    this.consultando.set(true);
    this.getAllClassrooms.execute({}).subscribe({
      next: aulas => {
        this.catalogo.set(aulas);
        this.consultando.set(false);
      },
      error: () => this.consultando.set(false)
    });
  }

  protected limpiar(): void {
    this.consulta.set('');
  }

  protected irAConsulta(): void {
    const texto = this.consulta().trim();
    void this.router.navigate(['/consulta'], texto === '' ? {} : { queryParams: { name: texto } });
    this.limpiar();
  }

  protected alternarMenu(): void {
    this.menuAbierto.update(abierto => !abierto);
  }

  protected cerrarSesion(): void {
    this.authService.logout();
    this.menuAbierto.set(false);
    void this.router.navigateByUrl('/login');
  }

  private esAtajoK(evento: KeyboardEvent): boolean {
    return (evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === 'k';
  }
}
