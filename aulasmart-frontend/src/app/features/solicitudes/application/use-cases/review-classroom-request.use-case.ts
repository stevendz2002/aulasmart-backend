import { Observable } from 'rxjs';

import { ClassroomRequestRepository } from '../../domain/repositories/classroom-request-repository';
import { ClassroomRequest, ReviewDecision } from '../../domain/entities/classroom-request';

/** Decide una solicitud pendiente: APROBAR o RECHAZAR (roles revisores). */
export class ReviewClassroomRequestUseCase {
  constructor(private readonly repository: ClassroomRequestRepository) {}

  execute(id: number, decision: ReviewDecision): Observable<ClassroomRequest> {
    return this.repository.review(id, decision);
  }
}
