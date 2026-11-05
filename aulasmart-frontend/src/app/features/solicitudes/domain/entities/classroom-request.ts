/* Entidades de dominio de las solicitudes de reserva.
   Puro TypeScript: sin Angular, HttpClient ni DTO externos. */

/** Estados reales del backend para una solicitud de aula. */
export type RequestStatus = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

/** Decisión de revisión tal como la espera el endpoint del backend. */
export type ReviewDecision = 'APROBAR' | 'RECHAZAR';

/** Solicitud de reserva de un aula en un intervalo [inicio, fin). */
export interface ClassroomRequest {
  id: number;
  classroomId: number;
  requesterId: number;
  start: string;
  end: string;
  status: RequestStatus;
  reason: string;
  reviewedById: number | null;
  reviewedAt: string | null;
  createdAt: string;
}

/** Datos que el usuario entrena al registrar una reserva. */
export interface CreateClassroomRequestCommand {
  classroomId: number;
  start: Date;
  end: Date;
  reason: string;
}
