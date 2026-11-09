import { of } from 'rxjs';
import { vi } from 'vitest';

import { GetUsersUseCase } from './get-users.use-case';
import { CreateUserAccountUseCase } from './create-user-account.use-case';
import { UserRepository } from '../../domain/repositories/user-repository';
import { CreateUserAccountCommand, User } from '../../domain/entities/user';

const usuario: User = {
  id: 9003,
  fullName: 'PRUEBA Administrador',
  email: 'admin@prueba.aulasmart.local',
  active: true,
  role: 'ADMINISTRADOR',
  createdAt: 'c'
};

function repositoryDouble(): UserRepository & {
  getAll: ReturnType<typeof vi.fn>;
  create: ReturnType<typeof vi.fn>;
} {
  return {
    getAll: vi.fn().mockReturnValue(of([usuario])),
    create: vi.fn().mockReturnValue(of(usuario))
  } as unknown as UserRepository & {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
  };
}

describe('Casos de uso de usuarios', () => {
  it('GetUsersUseCase delega en el repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new GetUsersUseCase(repository);

    useCase.execute().subscribe();

    expect(repository.getAll).toHaveBeenCalledExactlyOnceWith();
  });

  it('CreateUserAccountUseCase pasa el comando completo al repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new CreateUserAccountUseCase(repository);
    const command: CreateUserAccountCommand = {
      fullName: 'Ana María Restrepo',
      email: 'ana@uceva.edu.co',
      password: 'claveSegura1',
      role: 'DOCENTE'
    };

    useCase.execute(command).subscribe();

    expect(repository.create).toHaveBeenCalledExactlyOnceWith(command);
  });
});
