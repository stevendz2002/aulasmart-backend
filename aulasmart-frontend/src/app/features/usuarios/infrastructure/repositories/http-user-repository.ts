import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../../../../core/api-url';
import { UserMapper } from '../mappers/user-mapper';
import { CreateUserDto, UserDto } from '../dto/user-dto';
import { UserRepository } from '../../domain/repositories/user-repository';
import { CreateUserAccountCommand, User } from '../../domain/entities/user';

/** Implementación HTTP del directorio de usuarios. */
@Injectable({ providedIn: 'root' })
export class HttpUserRepository implements UserRepository {
  private readonly http = inject(HttpClient);

  getAll(): Observable<User[]> {
    return this.http
      .get<UserDto[]>(`${apiUrl}/usuarios`)
      .pipe(map(dtos => dtos.map(UserMapper.toUser)));
  }

  create(command: CreateUserAccountCommand): Observable<User> {
    return this.http
      .post<UserDto>(`${apiUrl}/usuarios`, UserMapper.toCreateDto(command))
      .pipe(map(UserMapper.toUser));
  }
}
