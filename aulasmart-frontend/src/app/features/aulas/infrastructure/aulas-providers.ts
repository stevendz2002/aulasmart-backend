import { Provider, inject } from '@angular/core';

import { HttpClassroomRepository } from './repositories/http-classroom-repository';
import { HttpSigaSyncRepository } from './repositories/http-siga-sync-repository';
import { GetAllClassroomsUseCase } from '../application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../application/use-cases/get-available-classrooms.use-case';
import { GetClassroomOccupationUseCase } from '../application/use-cases/get-classroom-occupation.use-case';
import { TriggerSigaSyncUseCase } from '../application/use-cases/trigger-siga-sync.use-case';
import {
  CLASSROOM_REPOSITORY,
  GET_ALL_CLASSROOMS_USE_CASE,
  GET_AVAILABLE_CLASSROOMS_USE_CASE,
  GET_CLASSROOM_OCCUPATION_USE_CASE,
  SIGA_SYNC_REPOSITORY,
  TRIGGER_SIGA_SYNC_USE_CASE
} from '../application/classrooms-di';

/**
 * Wiring de DI del feature aulas (Dependency Inversion): la abstracción vive
 * en el dominio, la implementación concreta en infraestructura y los casos de
 * uso se inyectan como dependencias de la presentación.
 */
export const AULAS_PROVIDERS: Provider[] = [
  { provide: CLASSROOM_REPOSITORY, useClass: HttpClassroomRepository },
  { provide: SIGA_SYNC_REPOSITORY, useClass: HttpSigaSyncRepository },
  {
    provide: GET_ALL_CLASSROOMS_USE_CASE,
    useFactory: () => new GetAllClassroomsUseCase(inject(CLASSROOM_REPOSITORY))
  },
  {
    provide: GET_AVAILABLE_CLASSROOMS_USE_CASE,
    useFactory: () => new GetAvailableClassroomsUseCase(inject(CLASSROOM_REPOSITORY))
  },
  {
    provide: GET_CLASSROOM_OCCUPATION_USE_CASE,
    useFactory: () => new GetClassroomOccupationUseCase(inject(CLASSROOM_REPOSITORY))
  },
  {
    provide: TRIGGER_SIGA_SYNC_USE_CASE,
    useFactory: () => new TriggerSigaSyncUseCase(inject(SIGA_SYNC_REPOSITORY))
  }
];
