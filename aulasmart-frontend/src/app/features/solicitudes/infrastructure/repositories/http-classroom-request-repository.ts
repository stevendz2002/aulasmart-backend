import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../../../../core/api-url';
import { ClassroomRequestMapper } from '../mappers/classroom-request-mapper';
import {
  ClassroomRequestDto,
  CreateClassroomRequestDto,
  ReviewDecisionDto
} from '../dto/classroom-request-dto';
import { ClassroomRequestRepository, RequestStatusFilter } from '../../domain/repositories/classroom-request-repository';
import {
  ClassroomRequest,
  CreateClassroomRequestCommand,
  ReviewDecision
} from '../../domain/entities/classroom-request';

/** Implementación HTTP del contrato del dominio. Único punto que toca la API. */
@Injectable({ providedIn: 'root' })
export class HttpClassroomRequestRepository implements ClassroomRequestRepository {
  private readonly http = inject(HttpClient);

  list(status?: RequestStatusFilter): Observable<ClassroomRequest[]> {
    const params = status && status !== 'ALL' ? { estado: status } : undefined;
    return this.http
      .get<ClassroomRequestDto[]>(`${apiUrl}/solicitudes`, { params })
      .pipe(map(dtos => dtos.map(ClassroomRequestMapper.toClassroomRequest)));
  }

  create(command: CreateClassroomRequestCommand): Observable<ClassroomRequest> {
    const body: CreateClassroomRequestDto = {
      aulaId: command.classroomId,
      inicio: command.start.toISOString(),
      fin: command.end.toISOString(),
      motivo: command.reason
    };
    return this.http.post<ClassroomRequestDto>(`${apiUrl}/solicitudes`, body)
      .pipe(map(ClassroomRequestMapper.toClassroomRequest));
  }

  review(id: number, decision: ReviewDecision): Observable<ClassroomRequest> {
    const body: ReviewDecisionDto = { decision };
    return this.http
      .post<ClassroomRequestDto>(`${apiUrl}/solicitudes/${id}/revision`, body)
      .pipe(map(ClassroomRequestMapper.toClassroomRequest));
  }
}
