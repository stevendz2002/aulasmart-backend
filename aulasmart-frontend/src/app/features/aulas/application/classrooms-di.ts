import { InjectionToken } from '@angular/core';

import { ClassroomRepository } from '../domain/repositories/classroom-repository';
import { SigaSyncRepository } from '../domain/repositories/siga-sync-repository';
import { GetAllClassroomsUseCase } from './use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from './use-cases/get-available-classrooms.use-case';
import { TriggerSigaSyncUseCase } from './use-cases/trigger-siga-sync.use-case';

/** Tokens de DI del feature aulas: la aplicación expone sus casos de uso
    como abstracciones que la presentación consume sin conocer HttpClient. */
export const CLASSROOM_REPOSITORY = new InjectionToken<ClassroomRepository>('CLASSROOM_REPOSITORY');
export const SIGA_SYNC_REPOSITORY = new InjectionToken<SigaSyncRepository>('SIGA_SYNC_REPOSITORY');

export const GET_ALL_CLASSROOMS_USE_CASE = new InjectionToken<GetAllClassroomsUseCase>(
  'GET_ALL_CLASSROOMS_USE_CASE'
);

export const GET_AVAILABLE_CLASSROOMS_USE_CASE = new InjectionToken<GetAvailableClassroomsUseCase>(
  'GET_AVAILABLE_CLASSROOMS_USE_CASE'
);

export const TRIGGER_SIGA_SYNC_USE_CASE = new InjectionToken<TriggerSigaSyncUseCase>(
  'TRIGGER_SIGA_SYNC_USE_CASE'
);
