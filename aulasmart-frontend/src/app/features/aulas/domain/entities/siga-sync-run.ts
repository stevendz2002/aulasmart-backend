/** Resumen de una corrida de sincronización con SIGA (contrato real del backend). */
export interface SigaSyncRun {
  /** Recurso sincronizado (la única plataforma sincronizable es AULAS). */
  resource: string;
  /** Estado reportado por el backend (ej.: «EXITOSA»). */
  status: string;
  recordsProcessed: number;
  startedAt: string;
  finishedAt: string | null;
  durationMilliseconds: number;
  errorDetail: string | null;
}
