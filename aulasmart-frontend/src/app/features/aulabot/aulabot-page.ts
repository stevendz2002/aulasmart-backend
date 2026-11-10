import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth-service';
import { BotReply, SendChatMessageUseCase } from './application/send-chat-message.use-case';
interface Turn { question: string; reply?: BotReply; error?: string; nuevaConversacion?: boolean; action?: string; }
@Component({
  selector: 'app-aulabot-page', imports: [FormsModule, RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush, styleUrl: './aulabot-page.css',
  template: `
    <section class="bot">
      <header><div><span class="eyebrow">Tu asistente de aulas</span><h2>AulaBot</h2>
      <p>Encuentra un aula y consulta su disponibilidad en hora Colombia.</p></div>
      <button type="button" class="secondary" (click)="reset()">Nueva conversación</button></header>
      <div class="conversation" role="log" aria-live="polite" aria-label="Conversación con AulaBot" [attr.aria-busy]="loading()">
        @if (!turns().length) {
          <div class="welcome"><span class="bot-icon" aria-hidden="true">✦</span><h3>¿Qué aula necesitas?</h3>
            <p>Escribe tu consulta en lenguaje natural o usa una acción rápida.</p>
            <div class="chips welcome-chips" aria-label="Sugerencias iniciales">
            @for (suggestion of suggestions(null); track suggestion) {
              <button class="secondary" type="button" [disabled]="loading()" (click)="pick(suggestion)">{{label(suggestion)}}</button>
            }
            </div>
          </div>
        }
        @for (turn of turns(); track $index) {
          <div class="question"><strong>Tú</strong><p>{{turn.question}}</p></div>
          <div class="answer"><strong>AulaBot</strong>
          @if (turn.reply; as reply) {
            <p>{{reply.texto}}</p>
            @if (reply.inicio) { <p class="date">{{reply.inicio | date:'dd/MM/yyyy HH:mm':'-0500'}} – {{reply.fin | date:'HH:mm':'-0500'}} · Colombia</p> }
            @if (reply.filtros) { <p class="filters">{{filterText(reply)}}</p> }
            @if (reply.verificadoEn) { <p class="muted">Verificado: {{reply.verificadoEn | date:'dd/MM/yyyy HH:mm':'-0500'}}</p> }
            <div class="rooms">
            @for (room of reply.aulas; track room.id) {
              <article><div class="room-top"><span class="room-icon" aria-hidden="true">▣</span>
                <span [class.free]="reply.estado === 'DISPONIBILIDAD'" class="badge">{{reply.estado === 'DISPONIBILIDAD' ? 'Libre en la franja' : 'Sin verificar'}}</span></div>
                <small>Cód. {{room.codigoSiga}}</small><h4>{{room.nombre}}</h4>
                <p>{{room.edificio.nombre}}</p><p>{{room.capacidad}} personas · {{room.tipoAula.nombre}}</p>
                <div class="actions"><a class="secondary" routerLink="/consulta" [queryParams]="{aula: room.id, name: room.nombre, accion: 'detalle', inicio: reply.inicio, fin: reply.fin}">Ver aula</a></div>
              </article>
            }
            </div>
            @if (suggestions(reply).length) {
              <div class="chips" aria-label="Sugerencias">
              @for (suggestion of suggestions(reply); track suggestion) {
                <button type="button" [disabled]="loading()" (click)="pick(suggestion)">{{label(suggestion)}}</button>
              }
              </div>
            }
          } @else if (turn.error) {
            <p role="alert">{{turn.error}}</p><button type="button" class="secondary" [disabled]="loading() || $index !== turns().length - 1" (click)="retry($index)">Reintentar</button>
            <a routerLink="/consulta">Ir a Consulta de aulas</a>
          } @else { <p role="status">Consultando…</p> }
          </div>
        }
      </div>
      <form (ngSubmit)="send()" class="composer">
        <label for="bot-message">Tu mensaje</label>
        <div><textarea id="bot-message" name="mensaje" [(ngModel)]="draft" rows="2" maxlength="600" [disabled]="loading()" placeholder="Un aula para 40 personas mañana de 08:00 a 10:00"></textarea>
        <button class="primary" type="submit" [disabled]="loading() || !draft.trim()">{{loading() ? 'Consultando…' : 'Enviar'}}</button></div>
        <small>Las acciones rápidas no consumen IA. Este chat es informativo: para solicitar una reserva usa la sección Solicitudes.</small>
      </form>
    </section>
  `
})
export class AulaBotPage {
  protected readonly auth = inject(AuthService);
  private readonly sendMessage = inject(SendChatMessageUseCase);
  private pending?: Subscription;
  private reiniciar = false;
  protected readonly turns = signal<Turn[]>([]);
  protected readonly loading = signal(false);
  protected draft = '';
  private readonly labels: Record<string, string> = {
    BUSCAR_AULA: 'Buscar un aula', CONSULTAR_DISPONIBILIDAD: 'Consultar disponibilidad',
    CAMBIAR_BLOQUE: 'Cambiar bloque', CAMBIAR_HORARIO: 'Cambiar horario', VER_OPCIONES: 'Ver otras opciones'
  };
  constructor() {
    let identity = this.auth.sesion()?.correo;
    effect(() => { const current = this.auth.sesion()?.correo; if (current !== identity) { identity = current; this.reset(); } });
    inject(DestroyRef).onDestroy(() => this.pending?.unsubscribe());
  }
  protected reset(): void {
    this.pending?.unsubscribe(); this.turns.set([]); this.loading.set(false); this.draft = ''; this.reiniciar = true;
  }
  /** Chips estructurados: el backend decide la acción sin llamar al modelo. */
  protected suggestions(reply: BotReply | null): string[] {
    if (reply === null) return ['BUSCAR_AULA', 'CONSULTAR_DISPONIBILIDAD', 'VER_OPCIONES'];
    return reply.sugerencias ?? [];
  }
  protected label(accion: string): string { return this.labels[accion] ?? accion; }
  protected pick(accion: string): void {
    if (this.loading()) return;
    const turn: Turn = { question: this.label(accion), action: accion, nuevaConversacion: false };
    this.turns.update(items => [...items.slice(-19), turn]);
    this.execute(this.turns().length - 1);
  }
  protected send(): void {
    const message = this.draft.trim();
    if (!message || message.length > 600 || this.loading()) return;
    const turn: Turn = { question: message, nuevaConversacion: this.reiniciar };
    this.reiniciar = false;
    this.turns.update(items => [...items.slice(-19), turn]); this.draft = '';
    this.execute(this.turns().length - 1);
  }
  protected retry(index: number): void { if (!this.loading() && index === this.turns().length - 1) this.execute(index); }
  private execute(index: number): void {
    const turn = this.turns()[index]; if (!turn) return;
    this.loading.set(true); this.update(index, {...turn, error: undefined});
    const request = turn.action
      ? this.sendMessage.executeAction(turn.action)
      : this.sendMessage.execute(turn.question, turn.nuevaConversacion === true);
    this.pending = request.subscribe({
      next: reply => {
        this.update(index, {...turn, reply});
        this.loading.set(false);
      },
      error: () => { this.update(index, {...turn,
        error: 'No pude completar la consulta. El servicio puede estar ocupado o haber alcanzado su cuota gratuita.'}); this.loading.set(false); }
    });
  }
  private update(index: number, turn: Turn): void { this.turns.update(items => items.map((value, i) => i === index ? turn : value)); }
  protected filterText(reply: BotReply): string {
    const f = reply.filtros ?? {};
    return [f['nombre'], f['edificio'], f['equipamiento'] ? 'Equipamiento: ' + f['equipamiento'] : null,
      f['tipoAula'], f['dependencia'], f['capacidadMinima'] ? 'Mínimo ' + f['capacidadMinima'] + ' personas' : null].filter(Boolean).join(' · ');
  }
}
