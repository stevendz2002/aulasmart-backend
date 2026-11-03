import { Observable } from 'rxjs';

import { ClassroomRepository } from '../../domain/repositories/classroom-repository';
import { Availability, ClassroomFilter } from '../../domain/entities/classroom';

/** Consulta las aulas disponibles en una franja [inicio, fin) con filtros. */
export class GetAvailableClassroomsUseCase {
  constructor(private readonly repository: ClassroomRepository) {}

  execute(start: Date, end: Date, filter: ClassroomFilter = {}): Observable<Availability> {
    return this.repository.getAvailable({ start, end }, filter);
  }
}
