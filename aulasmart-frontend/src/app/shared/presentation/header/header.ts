import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth-service';
import { ACADEMIC_PERIOD } from '../../../core/config/academic-period';

/**
 * Barra superior del shell: subtítulo institucional a la izquierda y sesión
 * real (correo y roles del `whoami`) a la derecha, con ingreso / salida.
 */
@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  styleUrl: './header.css',
  template: `
    <header class="barra">
      <span class="subtitulo">Gestión de reservas de aulas · UCEVA</span>

      @if (authService.sesion(); as sesion) {
        <div class="sesion">
          <span class="correo" [title]="sesion.roles.join(', ')">{{ sesion.correo }}</span>
          <span class="roles">{{ sesion.roles.join(' · ') }}</span>
        </div>
        <button type="button" class="salir" (click)="cerrarSesion()">Salir</button>
      } @else {
        <div class="sesion">
          <span class="roles">Sin sesión</span>
        </div>
        <a routerLink="/login" class="ingresar">Ingresar</a>
      }
    </header>
  `
})
export class Header {
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);
  protected readonly periodo = ACADEMIC_PERIOD;

  protected cerrarSesion(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
