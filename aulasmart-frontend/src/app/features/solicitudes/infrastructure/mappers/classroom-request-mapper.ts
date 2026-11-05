import { ClassroomRequestDto } from '../dto/classroom-request-dto';
import { ClassroomRequest } from '../../domain/entities/classroom-request';

/**
 * Adapter (patrón Adapter): traduce el DTO del backend (español, formas
 * externas) a la entidad de dominio en inglés, aislándolos entre sí.
 */
export class ClassroomRequestMapper {
  static toClassroomRequest(dto: ClassroomRequestDto): ClassroomRequest {
    return {
      id: dto.id,
      classroomId: dto.aulaId,
      requesterId: dto.docenteId,
      start: dto.inicio,
      end: dto.fin,
      status: dto.estado,
      reason: dto.motivo,
      reviewedById: dto.revisadaPorId,
      reviewedAt: dto.revisadaEn,
      createdAt: dto.creadaEn
    };
  }
}
