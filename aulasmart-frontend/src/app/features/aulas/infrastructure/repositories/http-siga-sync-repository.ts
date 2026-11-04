import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../../../../core/api-url';
import { SigaSyncMapper } from '../mappers/siga-sync-mapper';
import { SigaSyncRunDto } from '../dto/siga-sync-dto';
import { SigaSyncRepository } from '../../domain/repositories/siga-sync-repository';
import { SigaSyncResource, SigaSyncRun } from '../../domain/entities/siga-sync-run';

/** Implementación HTTP del contrato de sincronizaciones SIGA. */
@Injectable({ providedIn: 'root' })
export class HttpSigaSyncRepository implements SigaSyncRepository {
  private readonly http = inject(HttpClient);

  trigger(resource: SigaSyncResource): Observable<SigaSyncRun[]> {
    return this.http
      .post<SigaSyncRunDto[]>(`${apiUrl}/sincronizaciones`, null, { params: { recurso: resource } })
      .pipe(map(dtos => dtos.map(SigaSyncMapper.toSigaSyncRun)));
  }
}
