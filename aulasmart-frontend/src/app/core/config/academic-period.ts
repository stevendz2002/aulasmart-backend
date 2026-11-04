/**
 * Configuración del período académico vigente (parámetro institucional de la
 * spec, §14). Fuente única de verdad para el semestre activo mientras el
 * backend no publique un endpoint de períodos académicos.
 */
export interface AcademicPeriodConfig {
  /** Etiqueta legible del período (ej.: «2026-2»). */
  label: string;
  /** Fecha ISO (YYYY-MM-DD) del primer día de clases. */
  startDate: string;
  /** Cantidad de semanas del período. */
  totalWeeks: number;
}

export const ACADEMIC_PERIOD: AcademicPeriodConfig = {
  label: '2026-2',
  startDate: '2026-08-10',
  totalWeeks: 16
};

/** Semana del período académico a partir de la fecha de inicio de clases. */
export function semanaDelPeriodo(periodo: AcademicPeriodConfig, ahora: Date): number | null {
  const inicio = new Date(`${periodo.startDate}T00:00:00`);
  if (Number.isNaN(inicio.getTime())) {
    return null;
  }
  const dias = Math.floor((ahora.getTime() - inicio.getTime()) / 86_400_000);
  const semana = Math.floor(dias / 7) + 1;
  return semana >= 1 && semana <= periodo.totalWeeks ? semana : null;
}
