import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../../../../core/api-url';
import { ClassroomMapper } from '../mappers/classroom-mapper';
import { AvailabilityDto, ClassroomDto, OccupationDto } from '../dto/classroom-dto';
import { ClassroomRepository } from '../../domain/repositories/classroom-repository';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';
import { ClassroomOccupation } from '../../domain/entities/occupation';

/**
 * Implementación HTTP del contrato del dominio. Es el único punto del feature
 * que conoce HttpClient y la forma real de la API: traduce el filtro del
 * dominio a los query params del backend verificados (`capacidadMinima`,
 * `idEdificio`, `inicio`, `fin`, …).
 */
@Injectable({ providedIn: 'root' })
export class HttpClassroomRepository implements ClassroomRepository {
  private readonly http = inject(HttpClient);

  getAll(filter: ClassroomFilter): Observable<Classroom[]> {
    return this.http
      .get<ClassroomDto[]>(`${apiUrl}/aulas`, { params: this.filterToParams(filter) })
      .pipe(map(dtos => dtos.map(ClassroomMapper.toClassroom)));
  }

  getAvailable(range: { start: Date; end: Date }, filter: ClassroomFilter): Observable<Availability> {
    const params = this.filterToParams(filter)
      .set('inicio', range.start.toISOString())
      .set('fin', range.end.toISOString());
    return this.http
      .get<AvailabilityDto>(`${apiUrl}/aulas/disponibles`, { params })
      .pipe(map(dto => ClassroomMapper.toAvailability(dto)));
  }

  getOccupation(classroomId: number, range: { start: Date; end: Date }): Observable<ClassroomOccupation[]> {
    const params = new HttpParams()
      .set('inicio', range.start.toISOString())
      .set('fin', range.end.toISOString());
    return this.http
      .get<OccupationDto[]>(`${apiUrl}/aulas/${classroomId}/ocupacion`, { params })
      .pipe(map(dtos => dtos.map(ClassroomMapper.toOccupation)));
  }

  /** Traducción filtro de dominio → parámetros de la API (contrato real). */
  private filterToParams(filter: ClassroomFilter): HttpParams {
    let params = new HttpParams();
    if (filter.minCapacity !== undefined && filter.minCapacity > 0) {
      params = params.set('capacidadMinima', filter.minCapacity);
    }
    if (filter.buildingId !== undefined) {
      params = params.set('idEdificio', filter.buildingId);
    }
    if (filter.typeId !== undefined) {
      params = params.set('idTipoAula', filter.typeId);
    }
    if (filter.departmentId !== undefined) {
      params = params.set('idDependencia', filter.departmentId);
    }
    return params;
  }
}
