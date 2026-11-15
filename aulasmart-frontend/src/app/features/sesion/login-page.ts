import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink, UrlTree } from '@angular/router';

import { AuthService } from '../../core/auth-service';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  styleUrl: './login-page.css',
  template: `
    <section class="pantalla" aria-labelledby="titulo-ingreso">
      <form class="tarjeta" (submit)="ingresar($event)">
        <a routerLink="/" class="marca">AulaSmart <span>· UCEVA</span></a>
        <h1 id="titulo-ingreso">Iniciar sesión</h1>
        <p class="introduccion">Usa tu cuenta institucional para acceder a las funciones de tu rol.</p>

        <div class="campos">
          <label for="correo">
            Correo institucional
            <input id="correo" type="email" autocomplete="username" placeholder="nombre@uceva.edu.co" required />
          </label>
          <label for="clave">
            Contraseña
            <span class="campo-clave">
              <input id="clave" [type]="mostrandoClave() ? 'text' : 'password'" autocomplete="current-password" required />
              <button type="button" class="alternar-clave" (click)="alternarClave()"
                [attr.aria-label]="mostrandoClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                {{ mostrandoClave() ? 'Ocultar' : 'Mostrar' }}
              </button>
            </span>
          </label>
        </div>

        @if (error(); as mensaje) {
          <p class="mensaje-error" role="alert">{{ mensaje }}</p>
        }
        <button type="submit" class="ingresar" [disabled]="cargando()">
          {{ cargando() ? 'Ingresando…' : 'Iniciar sesión' }}
        </button>
        <a class="continuar" [routerLink]="rutaPublica()">Continuar sin iniciar sesión</a>
        <p class="soporte">¿Necesitas una cuenta o ayuda con el acceso? Contacta al administrador.</p>
      </form>
    </section>
  `
})
export class LoginPage {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrandoClave = signal(false);

  protected alternarClave(): void {
    this.mostrandoClave.update(activa => !activa);
  }

  private destino(): string {
    const volver = this.route.snapshot.queryParamMap.get('volver') ?? '';
    return /^\/(?:consulta|aulabot|solicitudes|aulas|usuarios)?(?:[?#]|$)/.test(volver) ? volver : '/';
  }

  protected rutaPublica(): UrlTree {
    const destino = this.destino();
    return this.router.parseUrl(/^\/(?:consulta|aulabot)(?:[?#]|$)/.test(destino) ? destino : '/');
  }

  protected ingresar(evento: Event): void {
    evento.preventDefault();
    if (this.cargando()) return;
    const formulario = evento.target as HTMLFormElement;
    const correo = formulario.querySelector<HTMLInputElement>('#correo')!.value.trim();
    const password = formulario.querySelector<HTMLInputElement>('#clave')!.value;

    this.cargando.set(true);
    this.error.set(null);
    this.authService.login(correo, password).subscribe({
      next: () => {
        this.cargando.set(false);
        void this.router.navigateByUrl(this.destino());
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(err.status === 401
          ? 'Credenciales inválidas. Revisa tu correo y contraseña.'
          : 'No fue posible iniciar sesión. Comprueba tu conexión e inténtalo de nuevo.');
      }
    });
  }
}
