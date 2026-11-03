import { Observable } from 'rxjs';

import { ClassroomRequestRepository } from '../../domain/repositories/classroom-request-repository';
import { ClassroomRequest, CreateClassroomRequestCommand } from '../../domain/entities/classroom-request';

/** Registra una solicitud de reserva de aula en el intervalo dado. */
export class CreateClassroomRequestUseCase {
  constructor(private readonly repository: ClassroomRequestRepository) {}

  execute(command: CreateClassroomRequestCommand): Observable<ClassroomRequest> {
    return this.repository.create(command);
  }
}
