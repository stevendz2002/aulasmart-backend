import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth-service';

@Component({
  selector: 'app-login-page',
  template: `
    <section class="login">
      <h2>Iniciar sesión</h2>
      <p>Usa tu correo institucional y su contraseña. El backend valida cada petición (HTTP Basic).</p>
      <form (submit)="ingresar($event)">
        <label for="correo">
          Correo
          <input id="correo" type="email" required autocomplete="username" placeholder="docente@prueba.aulasmart.local" />
        </label>
        <label for="clave">
          Contraseña
          <input id="clave" type="password" required autocomplete="current-password" />
        </label>
        <button type="submit" [disabled]="cargando()">Ingresar</button>
        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </form>
    </section>
  `,
  styles: `
    .login { max-width: 420px; display: flex; flex-direction: column; gap: 8px; }
    form { display: flex; flex-direction: column; gap: 12px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 0.9rem; color: #36506b; }
    input { padding: 8px 10px; border: 1px solid #c6cfdd; border-radius: 6px; font: inherit; }
    button { align-self: flex-start; padding: 8px 18px; border: 0; border-radius: 6px;
             background: #1f4e79; color: #fff; font: inherit; cursor: pointer; }
    button:disabled { opacity: 0.6; cursor: wait; }
    .error { color: #b3261e; }
  `
})
export class LoginPage {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected ingresar(evento: Event): void {
    evento.preventDefault();
    const formulario = evento.target as HTMLFormElement;
    const correo = (formulario.querySelector<HTMLInputElement>('#correo') as HTMLInputElement).value;
    const password = (formulario.querySelector<HTMLInputElement>('#clave') as HTMLInputElement).value;

    this.cargando.set(true);
    this.error.set(null);
    this.authService.login(correo, password).subscribe({
      next: () => {
        this.cargando.set(false);
        void this.router.navigateByUrl('/aulas');
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
