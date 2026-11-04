import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../../../../core/api-url';
import { UserMapper } from '../mappers/user-mapper';
import { CreateUserDto, UpdateUserDto, UserDto } from '../dto/user-dto';
import { UserRepository } from '../../domain/repositories/user-repository';
import { CreateUserAccountCommand, UpdateUserAccountCommand, User } from '../../domain/entities/user';

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

  update(id: number, changes: UpdateUserAccountCommand): Observable<User> {
    return this.http
      .patch<UserDto>(`${apiUrl}/usuarios/${id}`, UserMapper.toUpdateDto(changes))
      .pipe(map(UserMapper.toUser));
  }
}
