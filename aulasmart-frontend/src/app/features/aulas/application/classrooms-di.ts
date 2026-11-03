import { InjectionToken } from '@angular/core';

import { ClassroomRepository } from '../domain/repositories/classroom-repository';
import { GetAllClassroomsUseCase } from './use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from './use-cases/get-available-classrooms.use-case';

/** Tokens de DI del feature aulas: la aplicación expone sus casos de uso
    como abstracciones que la presentación consume sin conocer HttpClient. */
export const CLASSROOM_REPOSITORY = new InjectionToken<ClassroomRepository>('CLASSROOM_REPOSITORY');

export const GET_ALL_CLASSROOMS_USE_CASE = new InjectionToken<GetAllClassroomsUseCase>(
  'GET_ALL_CLASSROOMS_USE_CASE'
);

export const GET_AVAILABLE_CLASSROOMS_USE_CASE = new InjectionToken<GetAvailableClassroomsUseCase>(
  'GET_AVAILABLE_CLASSROOMS_USE_CASE'
);
