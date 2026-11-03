import { Observable } from 'rxjs';

import { ClassroomRepository } from '../../domain/repositories/classroom-repository';
import { Classroom, ClassroomFilter } from '../../domain/entities/classroom';

/** Consulta el catálogo de aulas aplicando los filtros indicados. */
export class GetAllClassroomsUseCase {
  constructor(private readonly repository: ClassroomRepository) {}

  execute(filter: ClassroomFilter = {}): Observable<Classroom[]> {
    return this.repository.getAll(filter);
  }
}
