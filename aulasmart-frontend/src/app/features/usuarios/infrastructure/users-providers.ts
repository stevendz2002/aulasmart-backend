import { Provider, inject } from '@angular/core';

import { HttpUserRepository } from './repositories/http-user-repository';
import { GetUsersUseCase } from '../application/use-cases/get-users.use-case';
import { CreateUserAccountUseCase } from '../application/use-cases/create-user-account.use-case';
import { UpdateUserAccountUseCase } from '../application/use-cases/update-user-account.use-case';
import {
  CREATE_USER_ACCOUNT_USE_CASE,
  GET_USERS_USE_CASE,
  UPDATE_USER_ACCOUNT_USE_CASE,
  USER_REPOSITORY
} from '../application/users-di';

/**
 * Wiring de DI del feature usuarios (Dependency Inversion): la abstracción
 * vive en el dominio, la implementación concreta en infraestructura y los
 * casos de uso se inyectan como dependencias de la presentación.
 */
export const USUARIOS_PROVIDERS: Provider[] = [
  { provide: USER_REPOSITORY, useClass: HttpUserRepository },
  { provide: GET_USERS_USE_CASE, useFactory: () => new GetUsersUseCase(inject(USER_REPOSITORY)) },
  {
    provide: CREATE_USER_ACCOUNT_USE_CASE,
    useFactory: () => new CreateUserAccountUseCase(inject(USER_REPOSITORY))
  },
  {
    provide: UPDATE_USER_ACCOUNT_USE_CASE,
    useFactory: () => new UpdateUserAccountUseCase(inject(USER_REPOSITORY))
  }
];
