import { Observable } from 'rxjs';

import { SigaSyncRepository } from '../../domain/repositories/siga-sync-repository';
import { SigaSyncResource, SigaSyncRun } from '../../domain/entities/siga-sync-run';

/** Dispara una sincronización SIGA vía el repositorio correspondiente. */
export class TriggerSigaSyncUseCase {
  constructor(private readonly repository: SigaSyncRepository) {}

  execute(resource: SigaSyncResource = 'AULAS'): Observable<SigaSyncRun[]> {
    return this.repository.trigger(resource);
  }
}
