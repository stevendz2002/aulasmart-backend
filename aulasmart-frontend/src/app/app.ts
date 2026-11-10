import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

import { Sidebar } from './shared/presentation/sidebar/sidebar';
import { Header } from './shared/presentation/header/header';
import { ThemeService } from './core/theme.service';

/** Shell de la SPA: menú lateral + topbar + contenido (sin chrome en el ingreso). */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, Header],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  /** Inicializa ThemeService para aplicar el tema al arrancar. */
  protected readonly themeService = inject(ThemeService);

  private readonly rutaActual = toSignal(
    this.router.events.pipe(
      filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
      map(evento => evento.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  /** La pantalla de ingreso se muestra completa, sin menú ni topbar. */
  protected readonly enIngreso = computed(() => this.rutaActual().split('?')[0].startsWith('/login'));
}
