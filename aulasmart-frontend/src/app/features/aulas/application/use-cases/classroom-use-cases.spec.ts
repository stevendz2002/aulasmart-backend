import { of } from 'rxjs';
import { vi } from 'vitest';

import { GetAllClassroomsUseCase } from './get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from './get-available-classrooms.use-case';
import { ClassroomRepository } from '../../domain/repositories/classroom-repository';
import { Availability, Classroom } from '../../domain/entities/classroom';

function repositoryDouble(): ClassroomRepository & { getAll: ReturnType<typeof vi.fn>; getAvailable: ReturnType<typeof vi.fn> } {
  const aula: Classroom = {
    id: 9,
    externalCode: 1,
    name: 'AULA PRUEBA',
    building: { id: 1, externalCode: 1, name: 'BLOQUE 1' },
    capacity: 40,
    type: { id: 2, externalCode: 1, name: 'SALON DE CLASE' },
    department: { id: 3, externalCode: 2, name: 'MATEMATICAS' }
  };
  const disponibilidad: Availability = {
    range: { start: 's', end: 'e', verifiedAt: 'v' },
    classrooms: [aula]
  };
  return {
    getAll: vi.fn().mockReturnValue(of([aula])),
    getAvailable: vi.fn().mockReturnValue(of(disponibilidad))
  } as never;
}

describe('Casos de uso del catálogo de aulas', () => {
  it('GetAllClassroomsUseCase delega el filtro en el repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new GetAllClassroomsUseCase(repository);

    useCase.execute({ minCapacity: 30 }).subscribe();

    expect(repository.getAll).toHaveBeenCalledExactlyOnceWith({ minCapacity: 30 });
  });

  it('GetAllClassroomsUseCase usa un filtro vacío por defecto', () => {
    const repository = repositoryDouble();
    const useCase = new GetAllClassroomsUseCase(repository);

    useCase.execute().subscribe();

    expect(repository.getAll).toHaveBeenCalledExactlyOnceWith({});
  });

  it('GetAvailableClassroomsUseCase pasa el rango y el filtro al repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new GetAvailableClassroomsUseCase(repository);

    useCase.execute(new Date('2026-11-10T14:00:00Z'), new Date('2026-11-10T16:00:00Z'), { minCapacity: 100 });

    expect(repository.getAvailable).toHaveBeenCalledExactlyOnceWith(
      { start: new Date('2026-11-10T14:00:00Z'), end: new Date('2026-11-10T16:00:00Z') },
      { minCapacity: 100 }
    );
  });
});
