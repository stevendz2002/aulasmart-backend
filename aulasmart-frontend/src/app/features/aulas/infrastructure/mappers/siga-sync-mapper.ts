import { SigaSyncRunDto } from '../dto/siga-sync-dto';
import { SigaSyncRun } from '../../domain/entities/siga-sync-run';

/** Adapter: DTO del backend (español) → entidad de dominio (inglés). */
export class SigaSyncMapper {
  static toSigaSyncRun(dto: SigaSyncRunDto): SigaSyncRun {
    return {
      resource: dto.recurso,
      status: dto.estado,
      recordsProcessed: dto.registrosProcesados,
      startedAt: dto.iniciadaEn,
      finishedAt: dto.finalizadaEn,
      durationMilliseconds: dto.duracionMilisegundos,
      errorDetail: dto.detalleError
    };
  }
}
