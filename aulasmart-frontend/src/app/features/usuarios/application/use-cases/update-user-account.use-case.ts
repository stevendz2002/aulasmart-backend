import { Observable } from 'rxjs';

import { UserRepository } from '../../domain/repositories/user-repository';
import { UpdateUserAccountCommand, User } from '../../domain/entities/user';

/** Edita o desactiva/reactiva una cuenta existente. */
export class UpdateUserAccountUseCase {
  constructor(private readonly repository: UserRepository) {}

  execute(id: number, changes: UpdateUserAccountCommand): Observable<User> {
    return this.repository.update(id, changes);
  }
}
