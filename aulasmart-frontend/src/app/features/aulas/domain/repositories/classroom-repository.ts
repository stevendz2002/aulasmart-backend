import { Observable } from 'rxjs';

import { Availability, Classroom, ClassroomFilter } from '../entities/classroom';
import { ClassroomOccupation } from '../entities/occupation';

export interface ClassroomRepository {
  getAll(filter: ClassroomFilter): Observable<Classroom[]>;
  getAvailable(range: { start: Date; end: Date }, filter: ClassroomFilter): Observable<Availability>;
  getOccupation(classroomId: number, range: { start: Date; end: Date }): Observable<ClassroomOccupation[]>;
}
