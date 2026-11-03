import { Component, ChangeDetectionStrategy } from '@angular/core';

import { ACADEMIC_PERIOD, AcademicPeriodConfig } from '../../../core/config/academic-period';

/** Semana del período académico a partir de la fecha de inicio de clases. */
export function semanaDelPeriodo(
  periodo: AcademicPeriodConfig,
  ahora: Date
): number | null {
  const inicio = new Date(`${periodo.startDate}T00:00:00`);
  if (Number.isNaN(inicio.getTime())) {
    return null;
  }
  const dias = Math.floor((ahora.getTime() - inicio.getTime()) / 86_400_000);
  const semana = Math.floor(dias / 7) + 1;
  return semana >= 1 && semana <= periodo.totalWeeks ? semana : null;
}

/**
 * Pie del shell: identidad del proyecto y período académico vigente
 * (config centralizado, spec §14) mientras aula está en semana activa.
 */
@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="pie">
      <small>
        AulaSmart · Reservas de aulas · UCEVA · Período académico {{ periodo.label }}
        @if (semana !== null) {
          · Semana {{ semana }} de {{ periodo.totalWeeks }}
        }
      </small>
    </footer>
  `,
  styles: `
    .pie {
      padding: 12px 24px;
      border-top: 1px solid var(--color-border);
      color: var(--color-text-secondary);
    }
  `
})
export class Footer {
  protected readonly periodo = ACADEMIC_PERIOD;
  protected readonly semana: number | null = semanaDelPeriodo(ACADEMIC_PERIOD, new Date());
}
