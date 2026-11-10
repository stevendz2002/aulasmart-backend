import { Injectable, inject } from '@angular/core';
import { AulaBotGateway } from './aulabot-gateway';
export type { BotReply } from '../domain/chat';
@Injectable({providedIn: 'root'})
export class SendChatMessageUseCase {
  private readonly repository = inject(AulaBotGateway);
  execute(message: string, nuevaConversacion: boolean) { return this.repository.send(message, nuevaConversacion); }
  executeAction(accion: string) { return this.repository.sendAction(accion); }
}
