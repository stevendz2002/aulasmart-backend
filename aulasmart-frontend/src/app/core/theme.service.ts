import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'aulasmart-theme';

  readonly theme = signal<Theme>(this.inicializar());

  constructor() {
    effect(() => {
      const t = this.theme();
      document.documentElement.setAttribute('data-theme', t);
      try { localStorage.setItem(this.STORAGE_KEY, t); } catch { /* storage no disponible */ }
    });
  }

  alternar(): void {
    this.theme.update(t => t === 'light' ? 'dark' : 'light');
  }

  private inicializar(): Theme {
    try {
      const guardado = localStorage.getItem(this.STORAGE_KEY) as Theme | null;
      if (guardado === 'light' || guardado === 'dark') return guardado;
    } catch { /* storage no disponible */ }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
