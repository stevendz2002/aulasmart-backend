/* Entidad de dominio: una franja ocupada de un aula en el calendario
   académico de SIGA (ficha de detalles). Puro TypeScript. */

export interface ClassroomOccupation {
  readonly start: string;
  readonly end: string;
  readonly details: string | null;
}
