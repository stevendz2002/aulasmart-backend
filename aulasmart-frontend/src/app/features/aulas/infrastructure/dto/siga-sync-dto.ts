/* Contratos JSON reales del backend para las corridas de sincronización. */

export interface SigaSyncRunDto {
  recurso: string;
  estado: string;
  registrosProcesados: number;
  iniciadaEn: string;
  finalizadaEn: string | null;
  duracionMilisegundos: number;
  detalleError: string | null;
}
