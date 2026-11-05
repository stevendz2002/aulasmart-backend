import { Observable } from 'rxjs';

import { UserRepository } from '../../domain/repositories/user-repository';
import { User } from '../../domain/entities/user';

/** Consulta el directorio completo de usuarios del sistema. */
export class GetUsersUseCase {
  constructor(private readonly repository: UserRepository) {}

  execute(): Observable<User[]> {
    return this.repository.getAll();
  }
}
