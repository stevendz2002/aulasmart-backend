import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';

import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../core/auth-service';
import {
  CREATE_CLASSROOM_REQUEST_USE_CASE,
  GET_CLASSROOM_REQUESTS_USE_CASE,
  REVIEW_CLASSROOM_REQUEST_USE_CASE
} from '../../application/classroom-requests-di';
import { CreateClassroomRequestUseCase } from '../../application/use-cases/create-classroom-request.use-case';
import { GetClassroomRequestsUseCase } from '../../application/use-cases/get-classroom-requests.use-case';
import { ReviewClassroomRequestUseCase } from '../../application/use-cases/review-classroom-request.use-case';
import { GET_ALL_CLASSROOMS_USE_CASE } from '../../../aulas/application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../../aulas/application/use-cases/get-all-classrooms.use-case';
import { Classroom } from '../../../aulas/domain/entities/classroom';
import { ClassroomRequest, ReviewDecision, RequestStatus } from '../../domain/entities/classroom-request';
import { RequestStatusFilter } from '../../domain/repositories/classroom-request-repository';

/** Pestaña de estado con su conteo real (etiqueta, valor y número). */
interface PestanaEstado {
  etiqueta: string;
  valor: RequestStatus | 'TODAS';
  cuenta: number;
}

function horaCorta(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-CO', { timeZone: 'America/Bogota', hour: '2-digit', minute: '2-digit' });
}

/**
 * Bandeja de solicitudes (diseño aprobado): pestanas con conteos reales por
 * estado, buscador por ID/aula/solicitante y tabla con acciones ✓/✗ para el
 * personal revisor. Los conteos se derivan de la única lista real de la API
 * (un pedido `ALL`) y el filtro es del lado del cliente: no se inventan
 * endpoints ni chiffres. DOCENTE conserva su formulario de registro y ve su
 * propio listado; para todos los roles el "solicitante" se muestra como
 * `Usuario id N` porque la API no expone nombres de personas.
 */
@Component({
  selector: 'app-solicitudes-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  styleUrl: './solicitudes-page.css',
  template: `
    <header class="cabecera">
      <div>
        <h2>Solicitudes de aula</h2>
        <p class="ayuda">
          @if (esDocente()) {
            Registra tu solicitud y sigue su estado: un revisor del área administrativa la aprobará o la rechazará.
          } @else if (esRevisor()) {
            Revisa las solicitudes pendientes y decide: aprobar registra una asignación local; no confirma una reserva institucional.
          } @else {
            Histórico de solicitudes de reserva de aulas.
          }
        </p>
      </div>
    </header>

    @if (puedeSolicitar()) {
      <section class="tarjeta registro">
        <header>
          <h3>Registrar nueva solicitud</h3>
          <span class="apagada">Horarios de Colombia. Una solicitud queda pendiente de revisión.</span>
        </header>
        <form class="formulario" (submit)="registrarReserva($event)">
          <label>
            Aula
            <select id="aulaNueva" required>
              <option value="" disabled selected>Seleccione…</option>
              @for (aula of aulasParaReserva(); track aula.id) {
                <option [value]="aula.id">{{ aula.name }} · capacidad {{ aula.capacity }}</option>
              }
            </select>
          </label>
          <label>
            Inicio
            <input id="inicioNueva" type="datetime-local" required [value]="inicioPorDefecto" />
          </label>
          <label>
            Fin
            <input id="finNueva" type="datetime-local" required [value]="finPorDefecto" />
          </label>
          <label>
            Motivo
            <textarea required id="motivoNueva" rows="2" maxlength="300" placeholder="Clase de refuerzo, monitoria, defensa de proyecto…"></textarea>
          </label>
          <button class="boton" type="submit" [disabled]="registrando()">
            {{ registrando() ? 'Enviando…' : 'Enviar solicitud' }}
          </button>
        </form>
        @if (mensajeRegistro(); as mensaje) {
          <p [class.mensaje-exito]="registroExitoso()" [class.mensaje-error]="!registroExitoso()">{{ mensaje }}</p>
        }
      </section>
    }

    <section class="tarjeta listado">
      <div class="controles">
        <button type="button" class="boton secundario" (click)="historial.set(!historial())">
          {{ historial() ? 'Ver solicitudes activas' : 'Ver historial (últimas 50)' }}
        </button>
        <div class="pestanas" role="tablist" aria-label="Filtrar por estado">
          @for (pestana of pestanas(); track pestana.valor) {
            <button
              type="button"
              role="tab"
              [class.activa]="pestanaActiva() === pestana.valor"
              [attr.aria-selected]="pestanaActiva() === pestana.valor"
              (click)="elegirPestana(pestana.valor)"
            >
              {{ pestana.etiqueta }} <span class="cuenta">{{ pestana.cuenta }}</span>
            </button>
          }
        </div>

        <label class="busqueda">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
            <path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16.2 16.2 21 21" />
          </svg>
          <input
            #entradaBusqueda
            type="search"
            placeholder="Buscar por ID, aula o solicitante…"
            aria-label="Buscar solicitudes"
            [value]="textoBusqueda()"
            (input)="buscar(entradaBusqueda.value)"
          />
        </label>
      </div>

      @if (mensajeRevision(); as mensaje) {
        <p [class.mensaje-exito]="revisionExitosa()" [class.mensaje-error]="!revisionExitosa()">{{ mensaje }}</p>
      }
      @if (errorCarga(); as mensaje) {
        <p class="mensaje-error">{{ mensaje }}</p>
      }

      <div class="tabla-envoltorio">
        @if (errorCarga()) {
          <button type="button" class="boton secundario" (click)="cargar('ALL')">Reintentar</button>
        } @else if (solicitudes() === null || cargando()) {
          <p class="mensaje-info">Consultando solicitudes…</p>
        } @else if (visibles().length === 0) {
          <p class="mensaje-info">
            @if (solicitudes()!.length === 0) {
              Sin solicitudes registradas todavía.
            } @else {
              Ninguna solicitud coincide con el filtro actual.
            }
          </p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Aula</th>
                <th>Fecha</th>
                <th>Horario</th>
                <th>Solicitante</th>
                <th>Motivo</th>
                <th>Estado</th>
                @if (esRevisor()) { <th>Acciones</th> }
              </tr>
            </thead>
            <tbody>
              @for (solicitud of visibles(); track solicitud.id) {
                <tr>
                  <td><code class="codigo">SOL-{{ solicitud.id }}</code></td>
                  <td class="aula">{{ nombreAula(solicitud.classroomId) }}</td>
                  <td>{{ fecha(solicitud.start) }}</td>
                  <td>{{ horaCorta(solicitud.start) }} – {{ horaCorta(solicitud.end) }}</td>
                  <td>Usuario id {{ solicitud.requesterId }}</td>
                  <td class="motivo">{{ solicitud.reason }}</td>
                  <td><span class="estado {{ solicitud.status }}">{{ solicitud.status }}</span></td>
                  @if (esRevisor()) {
                    <td class="acciones">
                      @if (solicitud.status === 'PENDIENTE' && !esHistorica(solicitud)) {
                        <button
                          type="button"
                          class="accion si"
                          title="Aprobar"
                          aria-label="Aprobar la solicitud SOL-{{ solicitud.id }}"
                          [disabled]="revisando().has(solicitud.id)"
                          (click)="decidir(solicitud, 'APROBAR')"
                        >✓</button>
                        <button
                          type="button"
                          class="accion no"
                          title="Rechazar"
                          aria-label="Rechazar la solicitud SOL-{{ solicitud.id }}"
                          [disabled]="revisando().has(solicitud.id)"
                          (click)="decidir(solicitud, 'RECHAZAR')"
                        >✕</button>
                      } @else {
                        <span class="resuelta">Resuelta</span>
                      }
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
          <p class="mensaje-info">{{ visibles().length }} de {{ solicitudes()!.length }} solicitud(es) listadas.</p>
        }
      </div>
    </section>
  `
})
export class SolicitudesPage implements OnInit, OnDestroy {
  private refresco?: ReturnType<typeof setInterval>;
  protected readonly historial = signal(false);
  protected esHistorica(s: ClassroomRequest): boolean {
    return s.status === 'RECHAZADA' || new Date(s.end).getTime() <= Date.now();
  }
  ngOnDestroy(): void { clearInterval(this.refresco); }
  private readonly route = inject(ActivatedRoute);
  protected readonly revisando = signal<Set<number>>(new Set());
  protected readonly puedeSolicitar = computed(() => this.esDocente() || this.esRevisor());
  private readonly authService = inject(AuthService);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getRequests = inject<GetClassroomRequestsUseCase>(GET_CLASSROOM_REQUESTS_USE_CASE);
  private readonly createRequest = inject<CreateClassroomRequestUseCase>(CREATE_CLASSROOM_REQUEST_USE_CASE);
  private readonly reviewRequest = inject<ReviewClassroomRequestUseCase>(REVIEW_CLASSROOM_REQUEST_USE_CASE);

  protected readonly aulasParaReserva = signal<Classroom[]>([]);
  protected readonly solicitudes = signal<ClassroomRequest[] | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<string | null>(null);

  protected readonly registrando = signal(false);
  protected readonly mensajeRegistro = signal<string | null>(null);
  protected readonly registroExitoso = signal(false);

  protected readonly mensajeRevision = signal<string | null>(null);
  protected readonly revisionExitosa = signal(false);

  protected readonly pestanaActiva = signal<RequestStatus | 'TODAS'>('TODAS');
  protected readonly textoBusqueda = signal('');

  protected readonly inicioPorDefecto = this.trasHoras(14);
  protected readonly finPorDefecto = this.trasHoras(16);

  private catalogoAulas: Classroom[] = [];

  protected readonly esDocente = computed(() => this.authService.tieneAlgunRol(['DOCENTE']));
  protected readonly esRevisor = computed(() =>
    this.authService.tieneAlgunRol(['ADMINISTRATIVO', 'ADMINISTRADOR'])
  );

  /** Conteos reales por estado a partir de la única lista de la API. */
  protected readonly pestanas = computed<PestanaEstado[]>(() => {
    const solicitudes = (this.solicitudes() ?? []).filter(s => this.esHistorica(s) === this.historial());
    const cuenta = (estado: RequestStatus) => solicitudes.filter(s => s.status === estado).length;
    return [
      { etiqueta: 'Todas', valor: 'TODAS', cuenta: solicitudes.length },
      { etiqueta: 'Pendientes', valor: 'PENDIENTE', cuenta: cuenta('PENDIENTE') },
      { etiqueta: 'Aprobadas', valor: 'APROBADA', cuenta: cuenta('APROBADA') },
      { etiqueta: 'Rechazadas', valor: 'RECHAZADA', cuenta: cuenta('RECHAZADA') }
    ];
  });

  /** Filtrado cliente por pestana + texto (ID, aula, solicitante o motivo). */
  protected readonly visibles = computed<ClassroomRequest[]>(() => {
    const lista = this.solicitudes() ?? [];
    const pestana = this.pestanaActiva();
    const texto = this.textoBusqueda().trim().toLowerCase();
    return lista.filter(solicitud => {
      if (this.esHistorica(solicitud) !== this.historial()) return false;
      if (pestana !== 'TODAS' && solicitud.status !== pestana) {
        return false;
      }
      if (texto === '') {
        return true;
      }
      if (/^sol-\d+$/.test(texto)) return solicitud.id === Number(texto.slice(4));
      const aguja = texto;
      return (
        String(solicitud.id).includes(aguja) ||
        this.nombreAula(solicitud.classroomId).toLowerCase().includes(aguja) ||
        `usuario id ${solicitud.requesterId}`.includes(aguja) ||
        solicitud.reason.toLowerCase().includes(aguja)
      );
    });
  });

  ngOnInit(): void {
    const id = this.route.snapshot.queryParamMap.get('solicitud');
    if (id && /^\d+$/.test(id)) this.textoBusqueda.set('SOL-' + id);
    this.getAllClassrooms.execute({}).subscribe({
      next: aulas => {
        this.aulasParaReserva.set(aulas);
        this.catalogoAulas = aulas;
      },
      error: () => {
        this.aulasParaReserva.set([]);
        this.catalogoAulas = [];
      }
    });
    this.cargar('ALL');
    this.refresco = setInterval(() => this.cargar('ALL'), 15000);
  }

  protected elegirPestana(valor: RequestStatus | 'TODAS'): void {
    this.pestanaActiva.set(valor);
  }

  protected buscar(texto: string): void {
    this.textoBusqueda.set(texto);
  }

  protected registrarReserva(evento: Event): void {
    evento.preventDefault();
    if (this.registrando() || !this.puedeSolicitar()) return;

    const aulaId = Number.parseInt(this.valor('#aulaNueva') ?? '', 10);
    const inicioTexto = this.valor('#inicioNueva');
    const finTexto = this.valor('#finNueva');
    const motivo = (this.valor('#motivoNueva') ?? '').trim();

    if (!Number.isFinite(aulaId) || !inicioTexto || !finTexto || !motivo) {
      this.registroExitoso.set(false);
      this.mensajeRegistro.set('Completa aula, inicio, fin y motivo de la solicitud.');
      return;
    }
    const inicio = new Date(inicioTexto + ':00-05:00');
    const fin = new Date(finTexto + ':00-05:00');
    if (!(inicio.getTime() < fin.getTime())) {
      this.registroExitoso.set(false);
      this.mensajeRegistro.set('El intervalo debe cumplir inicio < fin (semántica [inicio, fin)).');
      return;
    }

    this.registrando.set(true);
    this.createRequest
      .execute({
        classroomId: aulaId,
        start: inicio,
        end: fin,
        reason: motivo
      })
      .subscribe({
        next: solicitud => {
          this.registrando.set(false);
          this.registroExitoso.set(true);
          this.mensajeRegistro.set(
            `Solicitud SOL-${solicitud.id} registrada como ${solicitud.status}, queda a la espera de revisión.`
          );
          this.textoBusqueda.set('');
          this.pestanaActiva.set('TODAS');
          this.cargar('ALL');
        },
        error: error => {
          this.registrando.set(false);
          this.registroExitoso.set(false);
          this.mensajeRegistro.set(this.mensajeDe(error, 'No fue posible registrar la solicitud.'));
        }
      });
  }

  protected decidir(solicitud: ClassroomRequest, decision: ReviewDecision): void {
    if (!this.esRevisor() || solicitud.status !== 'PENDIENTE' || this.revisando().has(solicitud.id)) return;
    this.revisando.update(ids => new Set(ids).add(solicitud.id));
    this.mensajeRevision.set(null);
    this.reviewRequest.execute(solicitud.id, decision).subscribe({
      next: respuesta => {
        this.solicitudes.update(lista => lista?.map(item => item.id === respuesta.id ? respuesta : item) ?? null);
        this.terminarRevision(solicitud.id);
        this.revisionExitosa.set(true);
        this.mensajeRevision.set(`Solicitud SOL-${respuesta.id}: ${respuesta.status}.`);
        this.pestanaActiva.set('TODAS');
        this.cargar('ALL');
      },
      error: error => {
        this.terminarRevision(solicitud.id);
        this.revisionExitosa.set(false);
        this.mensajeRevision.set(
          this.mensajeDe(
            error,
            decision === 'APROBAR'
              ? 'La base rechazó la aprobación (por ejemplo, solape con otra reservada).'
              : 'No fue posible registrar la decisión.'
          )
        );
      }
    });
  }

  private terminarRevision(id: number): void {
    this.revisando.update(ids => { const copia = new Set(ids); copia.delete(id); return copia; });
  }

  protected nombreAula(classroomId: number): string {
    const aula = this.catalogoAulas.find(candidata => candidata.id === classroomId);
    return aula ? aula.name : `aula ${classroomId}`;
  }

  protected fecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CO', { timeZone: 'America/Bogota', day: '2-digit', month: 'short', year: 'numeric' });
  }

  protected horaCorta(iso: string): string {
    return horaCorta(iso);
  }

  private consultaListadoId = 0;

  protected cargar(estado: RequestStatusFilter): void {
    const consultaId = ++this.consultaListadoId;
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.getRequests.execute(estado).subscribe({
      next: solicitudes => {
        if (consultaId !== this.consultaListadoId) return;
        this.solicitudes.set(solicitudes);
        this.cargando.set(false);
      },
      error: error => {
        if (consultaId !== this.consultaListadoId) return;
        this.cargando.set(false);
        this.errorCarga.set(this.mensajeDe(error, 'No fue posible consultar las solicitudes.'));
      }
    });
  }

  private mensajeDe(error: { status?: number; error?: { message?: string } | null }, porDefecto: string): string {
    if (error.status === 503) {
      return 'No podemos verificar la disponibilidad en este momento. Intenta la operación más tarde.';
    }
    if (error.status === 401) {
      return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    }
    if (error.status === 403) {
      return 'Tu rol no permite esta operación.';
    }
    const detalle = error.error?.message;
    return detalle ? `${porDefecto} Detalle: ${detalle}` : porDefecto;
  }

  private valor(selector: string): string | null {
    const elemento = document.querySelector(selector) as HTMLInputElement | HTMLSelectElement | null;
    return elemento === null ? null : elemento.value;
  }

  private trasHoras(hora: number): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 1);
    const rellena = (valor: number) => String(valor).padStart(2, '0');
    return `${fecha.getFullYear()}-${rellena(fecha.getMonth() + 1)}-${rellena(fecha.getDate())}T${rellena(hora)}:00`;
  }
}
