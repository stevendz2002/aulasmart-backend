import { Observable } from 'rxjs';

import { SigaSyncResource, SigaSyncRun } from '../entities/siga-sync-run';

/** Contrato para disparar sincronizaciones SIGA (rol ADMINISTRATIVO+). */
export interface SigaSyncRepository {
  /** Lanza la sincronización de un recurso y devuelve las corridas resultantes. */
  trigger(resource: SigaSyncResource): Observable<SigaSyncRun[]>;
}
