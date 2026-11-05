import { Observable } from 'rxjs';

import { UserRepository } from '../../domain/repositories/user-repository';
import { CreateUserAccountCommand, User } from '../../domain/entities/user';

/**
 * Crea una cuenta con los datos del formulario. Quién puede invocarlo lo
 * decide el backend (solo ADMINISTRADOR); la SPA no autorregistro personas.
 */
export class CreateUserAccountUseCase {
  constructor(private readonly repository: UserRepository) {}

  execute(command: CreateUserAccountCommand): Observable<User> {
    return this.repository.create(command);
  }
}
