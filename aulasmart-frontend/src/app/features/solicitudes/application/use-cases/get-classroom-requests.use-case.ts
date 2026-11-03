import { Observable } from 'rxjs';

import { ClassroomRequestRepository } from '../../domain/repositories/classroom-request-repository';
import { RequestStatusFilter } from '../../domain/repositories/classroom-request-repository';
import { ClassroomRequest } from '../../domain/entities/classroom-request';

/** Lista solicitudes (todas, o las propias según rol, el backend decide). */
export class GetClassroomRequestsUseCase {
  constructor(private readonly repository: ClassroomRequestRepository) {}

  execute(filter: RequestStatusFilter = 'ALL'): Observable<ClassroomRequest[]> {
    return this.repository.list(filter);
  }
}
