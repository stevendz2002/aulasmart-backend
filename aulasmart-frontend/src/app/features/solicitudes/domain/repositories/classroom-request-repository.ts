import { Observable } from 'rxjs';

import {
  ClassroomRequest,
  CreateClassroomRequestCommand,
  RequestStatus,
  ReviewDecision
} from '../entities/classroom-request';

/**
 * Contrato de acceso a las solicitudes de reserva. La implementación HTTP
 * vive en infraestructura; casos de uso y pruebas dependen de la abstracción.
 */
export interface ClassroomRequestRepository {
  /** Lista solicitudes, opcionalmente filtradas por estado. */
  list(status?: RequestStatusFilter): Observable<ClassroomRequest[]>;

  /** Registra una solicitud (solo DOCENTE, lo refuerza el backend). */
  create(command: CreateClassroomRequestCommand): Observable<ClassroomRequest>;

  /** Decide una solicitud pendiente (solo revisores). */
  review(id: number, decision: ReviewDecision): Observable<ClassroomRequest>;
}

/** Filtro de listado: todas o por un estado concreto. */
export type RequestStatusFilter = 'ALL' | RequestStatus;
