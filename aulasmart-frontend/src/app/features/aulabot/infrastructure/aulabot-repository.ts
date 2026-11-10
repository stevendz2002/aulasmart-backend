import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { apiUrl } from '../../../core/api-url';
import type { BotReply } from '../domain/chat';
import { AulaBotGateway } from '../application/aulabot-gateway';
@Injectable({providedIn: 'root'})
export class AulaBotRepository implements AulaBotGateway {
  private readonly http = inject(HttpClient);
  send(mensaje: string, nuevaConversacion: boolean) {
    return this.http.post<BotReply>(apiUrl + '/aulabot/mensajes',{mensaje,nuevaConversacion},{timeout: 25000});
  }
  sendAction(accion: string) {
    return this.http.post<BotReply>(apiUrl + '/aulabot/mensajes',{accion},{timeout: 25000});
  }
}
