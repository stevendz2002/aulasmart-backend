import { Injectable, signal } from '@angular/core';

/** Estado visual del shell (compartido por topbar y sidebar). */
@Injectable({ providedIn: 'root' })
export class ShellState {
  /** Barra lateral colapsada a íconos. */
  readonly colapsada = signal(false);

  alternarColapso(): void {
    this.colapsada.update(estado => !estado);
  }
}
