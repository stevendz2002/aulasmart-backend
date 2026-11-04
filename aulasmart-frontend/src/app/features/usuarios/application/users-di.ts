import { InjectionToken } from '@angular/core';

import { UserRepository } from '../domain/repositories/user-repository';
import { GetUsersUseCase } from './use-cases/get-users.use-case';
import { CreateUserAccountUseCase } from './use-cases/create-user-account.use-case';

/** Tokens de DI del feature usuarios. */
export const USER_REPOSITORY = new InjectionToken<UserRepository>('USER_REPOSITORY');

export const GET_USERS_USE_CASE = new InjectionToken<GetUsersUseCase>('GET_USERS_USE_CASE');

export const CREATE_USER_ACCOUNT_USE_CASE = new InjectionToken<CreateUserAccountUseCase>(
  'CREATE_USER_ACCOUNT_USE_CASE'
);
