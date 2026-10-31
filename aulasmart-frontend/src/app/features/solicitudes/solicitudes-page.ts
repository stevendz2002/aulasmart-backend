import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { SolicitudesData } from './solicitudes-data';
import { AuthService } from '../../core/auth-service';
import { AulasData } from '../aulas/aulas-data';
import { Aula, DecisionRevision, Solicitud } from '../../core/modelos';

@Component({
  selector: 'app-solicitudes-page',
  template: `
    <section class="seccion">
      <h2>Solicitudes de reserva</h2>
      <p class="ayuda">
        @if (esDocente()) {
          Partes como <strong>DOCENTE</strong>: registra tus reservas y consulta su estado.
        } @else if (esRevisor()) {
          Partes como <strong>{{ sesion()?.roles?.join(', ') }}</strong>: decide las solicitudes pendientes.
        } @else {
          Consulta del histórico de solicitudes.
        }
      </p>
    </section>

    @if (esDocente()) {
      <section class="seccion">
        <h3>Registrar nueva solicitud</h3>
        <form class="formulario" (submit)="registrarReserva($event)">
          <label>
            Aula
            <select id="aulaNueva" required>
              <option value="" disabled selected>Seleccione…</option>
              @for (aula of aulasParaReserva(); track aula.id) {
                <option [value]="aula.id">{{ aula.nombre }} · capacidad {{ aula.capacidad }}</option>
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
            <textarea id="motivoNueva" rows="2" maxlength="300" placeholder="Clase de refuerzo, monitoria, defensa de proyecto…"></textarea>
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

    <section class="seccion">
      <h3>{{ esDocente() ? 'Mis solicitudes' : esRevisor() ? 'Bandeja de revisión' : 'Solicitudes' }}</h3>
      <form class="formulario" (submit)="actualizarEstado($event)">
        <label>
          Estado
          <select id="filtroEstado">
            <option value="TODAS" selected>Todas</option>
            <option value="PENDIENTE">Pendientes</option>
            <option value="APROBADA">Aprobadas</option>
            <option value="RECHAZADA">Rechazadas</option>
          </select>
        </label>
        <button class="boton secundario" type="submit" [disabled]="cargando()">
          {{ cargando() ? 'Consultando…' : 'Aplicar' }}
        </button>
      </form>

      @if (mensajeRevision(); as mensaje) {
        <p [class.mensaje-exito]="revisionExitosa()" [class.mensaje-error]="!revisionExitosa()">{{ mensaje }}</p>
      }
      @if (errorCarga(); as mensaje) {
        <p class="mensaje-error">{{ mensaje }}</p>
      }

      @if (solicitudes(); as lista) {
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Aula</th>
              <th>Inicio</th>
              <th>Fin</th>
              <th>Estado</th>
              <th>Motivo</th>
              @if (esRevisor()) { <th>Decisión</th> }
            </tr>
          </thead>
          <tbody>
            @for (solicitud of lista; track solicitud.id) {
              <tr>
                <td>{{ solicitud.id }}</td>
                <td>{{ nombreAula(solicitud.aulaId) }}</td>
                <td>{{ comoHora(solicitud.inicio) }}</td>
                <td>{{ comoHora(solicitud.fin) }}</td>
                <td><span class="estado {{ solicitud.estado }}">{{ solicitud.estado }}</span></td>
                <td>{{ solicitud.motivo }}</td>
                @if (esRevisor()) {
                  <td>
                    @if (solicitud.estado === 'PENDIENTE') {
                      <button class="boton ok" type="button" (click)="decidir(solicitud, 'APROBAR')">Aprobar</button>
                      <button class="boton no" type="button" (click)="decidir(solicitud, 'RECHAZAR')">Rechazar</button>
                    } @else {
                      revisada {{ revisadaEn(solicitud) }}
                    }
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
        <p class="mensaje-info">{{ lista.length }} solicitud(es).</p>
      }
    </section>
  `
})
export class SolicitudesPage implements OnInit {
  private readonly solicitudesData = inject(SolicitudesData);
  private readonly aulasData = inject(AulasData);
  protected readonly authService = inject(AuthService);

  protected readonly aulasParaReserva = signal<Aula[]>([]);
  protected readonly solicitudes = signal<Solicitud[] | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<string | null>(null);

  protected readonly registrando = signal(false);
  protected readonly mensajeRegistro = signal<string | null>(null);
  protected readonly registroExitoso = signal(false);

  protected readonly mensajeRevision = signal<string | null>(null);
  protected readonly revisionExitosa = signal(false);

  protected readonly inicioPorDefecto = this.trasHoras(14);
  protected readonly finPorDefecto = this.trasHoras(16);

  protected readonly esDocente = computed(() => this.authService.tieneAlgunRol(['DOCENTE']));
  protected readonly esRevisor = computed(() =>
    this.authService.tieneAlgunRol(['ADMINISTRATIVO', 'ADMINISTRADOR']));

  protected sesion() {
    return this.authService.sesion();
  }

  private catalogoAulas: Aula[] = [];

  ngOnInit(): void {
    if (this.esDocente()) {
      this.aulasData.listar({}).subscribe({
        next: aulas => (this.aulasParaReserva.set(aulas), (this.catalogoAulas = aulas)),
        error: () => (this.catalogoAulas = [])
      });
    }
    this.cargar('');
  }

  protected actualizarEstado(evento: Event): void {
    evento.preventDefault();
    const estado = (document.querySelector('#filtroEstado') as HTMLSelectElement | null)?.value ?? '';
    this.cargar(estado);
  }

  protected registrarReserva(evento: Event): void {
    evento.preventDefault();

    const aulaId = Number.parseInt(this.valor('#aulaNueva') ?? '', 10);
    const inicioTexto = this.valor('#inicioNueva');
    const finTexto = this.valor('#finNueva');
    const motivo = (this.valor('#motivoNueva') ?? '').trim();

    if (!Number.isFinite(aulaId) || !inicioTexto || !finTexto) {
      this.registroExitoso.set(false);
      this.mensajeRegistro.set('Completa aula, inicio y fin de la reserva.');
      return;
    }
    if (new Date(inicioTexto).getTime() >= new Date(finTexto).getTime()) {
      this.registroExitoso.set(false);
      this.mensajeRegistro.set('El intervalo debe cumplir inicio < fin (semántica [inicio, fin)).');
      return;
    }

    this.registrando.set(true);
    this.solicitudesData
      .crear({ aulaId, inicio: new Date(inicioTexto), fin: new Date(finTexto), motivo })
      .subscribe({
        next: solicitud => {
          this.registrando.set(false);
          this.registroExitoso.set(true);
          this.mensajeRegistro.set(
            `Solicitud #${solicitud.id} registrada como ${solicitud.estado}, queda a la espera de revisión.`
          );
          this.cargar('');
        },
        error: error => {
          this.registrando.set(false);
          this.registroExitoso.set(false);
          this.mensajeRegistro.set(this.mensajeDe(error, 'No fue posible registrar la solicitud.'));
        }
      });
  }

  protected decidir(solicitud: Solicitud, decision: DecisionRevision): void {
    this.solicitudesData.revisar(solicitud.id, decision).subscribe({
      next: respuesta => {
        this.revisionExitosa.set(true);
        this.mensajeRevision.set(
          `Solicitud #${respuesta.id}: ${respuesta.estado}.`
        );
        this.cargar('');
      },
      error: error => {
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

  protected nombreAula(aulaId: number): string {
    const aula = this.catalogoAulas.find(candidata => candidata.id === aulaId);
    return aula ? aula.nombre : `aula ${aulaId}`;
  }

  protected comoHora(iso: string): string {
    return new Date(iso).toLocaleString();
  }

  protected revisadaEn(solicitud: Solicitud): string {
    return solicitud.revisadaEn === null ? '—' : this.comoHora(solicitud.revisadaEn);
  }

  private cargar(estado: string): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.solicitudesData.listar(estado || undefined).subscribe({
      next: solicitudes => {
        this.solicitudes.set(solicitudes);
        this.cargando.set(false);
      },
      error: error => {
        this.cargando.set(false);
        this.errorCarga.set(this.mensajeDe(error, 'No fue posible consultar las solicitudes.'));
      }
    });
  }

  private mensajeDe(error: HttpErrorResponse, porDefecto: string): string {
    if (error.status === 401) {
      return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    }
    if (error.status === 403) {
      return 'Tu rol no permite esta operación.';
    }
    const detalle = (error.error as { message?: string } | null | undefined)?.message;
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
