import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth-service';

type CeldaIlustrativa = 'libre' | 'ocupada' | 'vacia';

/** Cuadrícula decorativa del panel oscuro: ilustración de marca, sin datos. */
const CELDAS_ILUSTRATIVAS: CeldaIlustrativa[] = [
  'vacia', 'libre', 'vacia', 'ocupada', 'ocupada', 'vacia', 'libre',
  'libre', 'libre', 'vacia', 'ocupada', 'vacia', 'vacia', 'ocupada',
  'vacia', 'ocupada', 'ocupada', 'libre', 'libre', 'vacia', 'libre',
  'ocupada', 'vacia', 'libre', 'libre', 'vacia', 'ocupada', 'vacia',
  'vacia', 'libre', 'vacia', 'ocupada', 'libre', 'libre', 'vacia'
];

/**
 * Ingreso (diseño aprobado): panel oscuro con la propuesta de valor y una
 * cuadrícula horaria decorativa (ilustración, no datos reales) y formulario
 * de inicio de sesión. Solo inicia sesión: las cuentas las crea el
 * administrador, por eso no hay registro ni recuperación aquí, solo la
 * indicación de contactar al administrador.
 */
@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './login-page.css',
  template: `
    <div class="pantalla">
      <section class="panel-oscuro" aria-hidden="true">
        <div class="marca">
          <span class="logotipo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 4 22 9.2l-10 5.2L2 9.2zM6.5 12v4.5c0 1.6 2.5 3 5.5 3s5.5-1.4 5.5-3V12" />
            </svg>
          </span>
          <span class="nombre">AulaSmart</span>
        </div>

        <h1>Encuentra tu aula libre en segundos.</h1>
        <p class="propuesta">
          El catálogo institucional sincronizado con SIGA, la disponibilidad real
          por franja y tus solicitudes de reserva, en una sola pantalla.
        </p>

        <div class="cuadricula">
          @for (celda of celdas; track $index) {
            <span class="celda {{ 'celda-' + celda }}"></span>
          }
        </div>

        <div class="leyenda">
          <span class="punto libre"></span>Libre
          <span class="punto ocupada"></span>Ocupada
          <span class="punto vacia"></span>Sin clase
        </div>

        <span class="nota-ilustracion">Ilustración del horario del campus</span>
      </section>

      <section class="panel-formulario">
        <form (submit)="ingresar($event)">
          <h2>Iniciar sesión</h2>
          <p class="introduccion">Ingresa con tu cuenta institucional UCEVA.</p>

          <label for="correo">
            Correo institucional
            <input id="correo" type="email" required autocomplete="username" placeholder="nombre@uceva.edu.co" />
          </label>

          <label for="clave">
            Contraseña
            <span class="campo-clave">
              <input
                id="clave"
                [type]="mostrandoClave() ? 'text' : 'password'"
                required
                autocomplete="current-password"
                placeholder="••••••••"
              />
              <button
                type="button"
                class="alternar-clave"
                (click)="alternarClave()"
                [attr.aria-pressed]="mostrandoClave()"
                [attr.aria-label]="mostrandoClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  @if (mostrandoClave()) {
                    <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" />
                    <circle cx="12" cy="12" r="2.6" />
                    <path d="M4.5 4.5 19.5 19.5" />
                  } @else {
                    <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" />
                    <circle cx="12" cy="12" r="2.6" />
                  }
                </svg>
              </button>
            </span>
          </label>

          @if (cargando()) {
            <p class="estado">Verificando credenciales…</p>
          }
          <button type="submit" class="ingresar" [disabled]="cargando()">
            {{ cargando() ? 'Ingresando…' : 'Iniciar sesión' }}
          </button>

          @if (error(); as mensaje) {
            <p class="mensaje-error">{{ mensaje }}</p>
          }

          <p class="soporte">
            ¿No tienes cuenta o perdiste el acceso? Contacta al administrador.
          </p>
        </form>

        <span class="pie">UCEVA · AulaSmart · 2026</span>
      </section>
    </div>
  `
})
export class LoginPage {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly celdas = CELDAS_ILUSTRATIVAS;
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrandoClave = signal(false);

  protected alternarClave(): void {
    this.mostrandoClave.update(activa => !activa);
  }

  protected ingresar(evento: Event): void {
    evento.preventDefault();
    const formulario = evento.target as HTMLFormElement;
    const correo = formulario.querySelector<HTMLInputElement>('#correo')!.value.trim();
    const password = formulario.querySelector<HTMLInputElement>('#clave')!.value;

    this.cargando.set(true);
    this.error.set(null);
    this.authService.login(correo, password).subscribe({
      next: () => {
        this.cargando.set(false);
        void this.router.navigateByUrl('/');
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(err.status === 401
          ? 'Credenciales inválidas: revisa tu correo y contraseña.'
          : `No fue posible iniciar sesión (HTTP ${err.status}).`);
      }
    });
  }
}
