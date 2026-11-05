import { Observable } from 'rxjs';

import { ClassroomRepository } from '../../domain/repositories/classroom-repository';
import { ClassroomOccupation } from '../../domain/entities/occupation';

export class GetClassroomOccupationUseCase {
  constructor(private readonly repository: ClassroomRepository) {}

  execute(classroomId: number, range: { start: Date; end: Date }): Observable<ClassroomOccupation[]> {
    return this.repository.getOccupation(classroomId, range);
  }
}
