import { Observable } from 'rxjs';
import { BotReply } from '../domain/chat';
export abstract class AulaBotGateway {
  /** nuevaConversacion: true sólo cuando el usuario reinicia; la memoria conversacional vive en el servidor. */
  abstract send(message: string, nuevaConversacion: boolean): Observable<BotReply>;
  /** Acción estructurada de un chip: no consume llamadas al modelo de lenguaje. */
  abstract sendAction(accion: string): Observable<BotReply>;
}
