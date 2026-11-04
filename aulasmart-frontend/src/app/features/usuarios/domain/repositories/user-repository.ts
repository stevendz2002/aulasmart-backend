/* Contrato del directorio de usuarios (abstracción del dominio):
    la implementación concreta vive en infraestructura. */

import { Observable } from 'rxjs';

import { CreateUserAccountCommand, User } from '../entities/user';

export interface UserRepository {
  /** Directorio completo: el backend solo lo entrega al ADMINISTRADOR. */
  getAll(): Observable<User[]>;

  /** Crear una cuenta: regla del negocio, solo ADMINISTRADOR en el backend. */
  create(command: CreateUserAccountCommand): Observable<User>;
}
