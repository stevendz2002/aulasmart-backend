import { InjectionToken } from '@angular/core';

import { ClassroomRequestRepository } from '../domain/repositories/classroom-request-repository';
import { GetClassroomRequestsUseCase } from './use-cases/get-classroom-requests.use-case';
import { CreateClassroomRequestUseCase } from './use-cases/create-classroom-request.use-case';
import { ReviewClassroomRequestUseCase } from './use-cases/review-classroom-request.use-case';

/** Tokens de DI del feature solicitudes. */
export const CLASSROOM_REQUEST_REPOSITORY = new InjectionToken<ClassroomRequestRepository>(
  'CLASSROOM_REQUEST_REPOSITORY'
);

export const GET_CLASSROOM_REQUESTS_USE_CASE = new InjectionToken<GetClassroomRequestsUseCase>(
  'GET_CLASSROOM_REQUESTS_USE_CASE'
);

export const CREATE_CLASSROOM_REQUEST_USE_CASE = new InjectionToken<CreateClassroomRequestUseCase>(
  'CREATE_CLASSROOM_REQUEST_USE_CASE'
);

export const REVIEW_CLASSROOM_REQUEST_USE_CASE = new InjectionToken<ReviewClassroomRequestUseCase>(
  'REVIEW_CLASSROOM_REQUEST_USE_CASE'
);
