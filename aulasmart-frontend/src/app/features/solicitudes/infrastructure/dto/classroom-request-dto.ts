/* Contratos JSON reales del backend para /api/solicitudes (verificados en vivo). */

import { RequestStatus, ReviewDecision } from '../../domain/entities/classroom-request';

export interface ClassroomRequestDto {
  id: number;
  aulaId: number;
  docenteId: number;
  inicio: string;
  fin: string;
  estado: RequestStatus;
  motivo: string;
  revisadaPorId: number | null;
  revisadaEn: string | null;
  creadaEn: string;
}

export interface CreateClassroomRequestDto {
  aulaId: number;
  inicio: string;
  fin: string;
  motivo: string;
}

export interface ReviewDecisionDto {
  decision: ReviewDecision;
}

export interface CreateRequestResponseDto {
  id: number;
  estado: string;
}
