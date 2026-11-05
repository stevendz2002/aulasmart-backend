import { Provider, inject } from '@angular/core';

import { HttpClassroomRequestRepository } from './repositories/http-classroom-request-repository';
import { GetClassroomRequestsUseCase } from '../application/use-cases/get-classroom-requests.use-case';
import { CreateClassroomRequestUseCase } from '../application/use-cases/create-classroom-request.use-case';
import { ReviewClassroomRequestUseCase } from '../application/use-cases/review-classroom-request.use-case';
import {
  CLASSROOM_REQUEST_REPOSITORY,
  CREATE_CLASSROOM_REQUEST_USE_CASE,
  GET_CLASSROOM_REQUESTS_USE_CASE,
  REVIEW_CLASSROOM_REQUEST_USE_CASE
} from '../application/classroom-requests-di';

/** Wiring de DI del feature solicitudes (Dependency Inversion). */
export const SOLICITUDES_PROVIDERS: Provider[] = [
  { provide: CLASSROOM_REQUEST_REPOSITORY, useClass: HttpClassroomRequestRepository },
  {
    provide: GET_CLASSROOM_REQUESTS_USE_CASE,
    useFactory: () => new GetClassroomRequestsUseCase(inject(CLASSROOM_REQUEST_REPOSITORY))
  },
  {
    provide: CREATE_CLASSROOM_REQUEST_USE_CASE,
    useFactory: () => new CreateClassroomRequestUseCase(inject(CLASSROOM_REQUEST_REPOSITORY))
  },
  {
    provide: REVIEW_CLASSROOM_REQUEST_USE_CASE,
    useFactory: () => new ReviewClassroomRequestUseCase(inject(CLASSROOM_REQUEST_REPOSITORY))
  }
];
