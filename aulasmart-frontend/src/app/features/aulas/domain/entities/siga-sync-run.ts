/** Recursos sincronizables reales del backend (POST /api/sincronizaciones). */
export type SigaSyncResource = 'AULAS' | 'EVENTOS';

/** Resumen de una corrida de sincronización con SIGA (contrato real del backend). */
export interface SigaSyncRun {
  /** Recurso sincronizado: AULAS (catálogo) o EVENTOS (ocupación por clases). */
  resource: string;
  /** Estado reportado por el backend (ej.: «EXITOSA»). */
  status: string;
  recordsProcessed: number;
  startedAt: string;
  finishedAt: string | null;
  durationMilliseconds: number;
  errorDetail: string | null;
}
