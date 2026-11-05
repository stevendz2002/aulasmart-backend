import { of } from 'rxjs';
import { vi } from 'vitest';

import { GetClassroomRequestsUseCase } from './get-classroom-requests.use-case';
import { CreateClassroomRequestUseCase } from './create-classroom-request.use-case';
import { ReviewClassroomRequestUseCase } from './review-classroom-request.use-case';
import { ClassroomRequestRepository } from '../../domain/repositories/classroom-request-repository';
import { ClassroomRequest } from '../../domain/entities/classroom-request';

const solicitud: ClassroomRequest = {
  id: 1,
  classroomId: 2,
  requesterId: 3,
  start: 's',
  end: 'e',
  status: 'PENDIENTE',
  reason: 'Clase de refuerzo',
  reviewedById: null,
  reviewedAt: null,
  createdAt: 'c'
};

function repositoryDouble(): ClassroomRequestRepository & { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; review: ReturnType<typeof vi.fn> } {
  return {
    list: vi.fn().mockReturnValue(of([solicitud])),
    create: vi.fn().mockReturnValue(of(solicitud)),
    review: vi.fn().mockReturnValue(of({ ...solicitud, status: 'APROBADA' }))
  } as unknown as ClassroomRequestRepository & { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; review: ReturnType<typeof vi.fn> };
}

describe('Casos de uso de solicitudes', () => {
  it('GetClassroomRequestsUseCase delega el filtro en el repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new GetClassroomRequestsUseCase(repository);

    useCase.execute('PENDIENTE').subscribe();

    expect(repository.list).toHaveBeenCalledExactlyOnceWith('PENDIENTE');
  });

  it('GetClassroomRequestsUseCase usa ALL por defecto', () => {
    const repository = repositoryDouble();
    const useCase = new GetClassroomRequestsUseCase(repository);

    useCase.execute().subscribe();

    expect(repository.list).toHaveBeenCalledExactlyOnceWith('ALL');
  });

  it('CreateClassroomRequestUseCase pasa el comando completo al repositorio', () => {
    const repository = repositoryDouble();
    const useCase = new CreateClassroomRequestUseCase(repository);
    const command = {
      classroomId: 9004,
      start: new Date('2026-11-11T14:00:00Z'),
      end: new Date('2026-11-11T16:00:00Z'),
      reason: 'Monitoria'
    };

    useCase.execute(command);

    expect(repository.create).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('ReviewClassroomRequestUseCase pasa la id y la decisión', () => {
    const repository = repositoryDouble();
    const useCase = new ReviewClassroomRequestUseCase(repository);

    useCase.execute(9002, 'RECHAZAR');

    expect(repository.review).toHaveBeenCalledExactlyOnceWith(9002, 'RECHAZAR');
  });
});
