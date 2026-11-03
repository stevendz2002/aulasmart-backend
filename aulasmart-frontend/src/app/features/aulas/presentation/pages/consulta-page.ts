import { Component, OnInit, WritableSignal, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../application/use-cases/get-available-classrooms.use-case';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';

@Component({
  selector: 'app-consulta-page',
  template: `
    <section class="seccion">
      <h2>Consulta de aulas</h2>
      <p class="ayuda">Encuentra un aula libre para la franja que necesitas. Catálogo sincronizado desde SIGA.</p>

      <form class="formulario" (submit)="consultarCatalogo($event)">
        <label>
          Nombre contiene
          <input id="buscarNombre" type="text" placeholder="salón, laboratorio…" />
        </label>
        <label>
          Capacidad mínima
          <input id="capacidadCatalogo" type="number" min="0" placeholder="0" />
        </label>
        <button class="boton" type="submit" [disabled]="cargandoCatalogo()">
          {{ cargandoCatalogo() ? 'Consultando…' : 'Consultar' }}
        </button>
      </form>

      @if (errorCatalogo()) {
        <p class="mensaje-error">{{ errorCatalogo() }}</p>
      }

      @if (aulas(); as lista) {
        <table>
          <thead>
            <tr>
              <th>Aula</th>
              <th>Edificio</th>
              <th>Tipo</th>
              <th>Dependencia</th>
              <th class="numero">Capacidad</th>
            </tr>
          </thead>
          <tbody>
            @for (aula of lista; track aula.id) {
              <tr>
                <td>{{ aula.name }}</td>
                <td>{{ aula.building.name }}</td>
                <td>{{ aula.type.name }}</td>
                <td>{{ aula.department.name }}</td>
                <td class="numero">{{ aula.capacity }}</td>
              </tr>
            }
          </tbody>
        </table>
        <p class="mensaje-info">{{ lista.length }} aula(s) listadas.</p>
      }
    </section>

    <section class="seccion">
      <h2>Disponibilidad</h2>
      <p class="ayuda">
        Intervalo <code>[inicio, fin)</code>: las reservas que solo se tocan en el borde no chocan.
        Solo cuentan las solicitudes APROBADAS.
      </p>

      <form class="formulario" (submit)="consultarDisponibilidad($event)">
        <label>
          Inicio
          <input id="inicioDisponibilidad" type="datetime-local" required [value]="inicioPorDefecto" />
        </label>
        <label>
          Fin
          <input id="finDisponibilidad" type="datetime-local" required [value]="finPorDefecto" />
        </label>
        <label>
          Capacidad mínima
          <input id="capacidadDisponibilidad" type="number" min="0" placeholder="0" />
        </label>
        <button class="boton" type="submit" [disabled]="cargandoDisponibilidad()">
          {{ cargandoDisponibilidad() ? 'Consultando…' : 'Buscar disponibles' }}
        </button>
      </form>

      @if (errorDisponibilidad()) {
        <p class="mensaje-error">{{ errorDisponibilidad() }}</p>
      }

      @if (disponibilidad(); as resultado) {
        <table>
          <thead>
            <tr>
              <th>Aula</th>
              <th>Edificio</th>
              <th class="numero">Capacidad</th>
            </tr>
          </thead>
          <tbody>
            @for (aula of resultado.classrooms; track aula.id) {
              <tr>
                <td>{{ aula.name }}</td>
                <td>{{ aula.building.name }}</td>
                <td class="numero">{{ aula.capacity }}</td>
              </tr>
            }
          </tbody>
        </table>
        <p class="mensaje-info">
          {{ resultado.classrooms.length }} aula(s) libres en el intervalo · verificado a las
          {{ comoHora(resultado.range.verifiedAt) }}.
        </p>
      }
    </section>
  `
})
export class ConsultaPage implements OnInit {
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(
    GET_AVAILABLE_CLASSROOMS_USE_CASE
  );

  private readonly catalogo = signal<Classroom[] | null>(null);
  private readonly nombreBuscado = signal('');

  protected readonly aulas = computed<Classroom[] | null>(() => {
    const todas = this.catalogo();
    if (todas === null) {
      return null;
    }
    const nombre = this.nombreBuscado().toLowerCase();
    return nombre === '' ? todas : todas.filter(aula => aula.name.toLowerCase().includes(nombre));
  });

  protected readonly cargandoCatalogo = signal(false);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly disponibilidad = signal<Availability | null>(null);
  protected readonly cargandoDisponibilidad = signal(false);
  protected readonly errorDisponibilidad = signal<string | null>(null);

  protected readonly inicioPorDefecto = this.trasHoras(14);
  protected readonly finPorDefecto = this.trasHoras(16);

  ngOnInit(): void {
    this.cargarCatalogo({});
  }

  protected consultarCatalogo(evento: Event): void {
    evento.preventDefault();
    this.nombreBuscado.set((this.valor('#buscarNombre') ?? '').trim());

    const capacidad = Number.parseInt(this.valor('#capacidadCatalogo') ?? '', 10);
    this.cargarCatalogo({ minCapacity: Number.isFinite(capacidad) && capacidad > 0 ? capacidad : undefined });
  }

  protected consultarDisponibilidad(evento: Event): void {
    evento.preventDefault();
    const inicio = this.valor('#inicioDisponibilidad');
    const fin = this.valor('#finDisponibilidad');
    if (!inicio || !fin) {
      this.errorDisponibilidad.set('Selecciona el rango de tiempo a consultar.');
      return;
    }

    const capacidad = Number.parseInt(this.valor('#capacidadDisponibilidad') ?? '', 10);
    this.cargandoDisponibilidad.set(true);
    this.errorDisponibilidad.set(null);
    this.getAvailableClassrooms
      .execute(new Date(inicio), new Date(fin), {
        minCapacity: Number.isFinite(capacidad) && capacidad > 0 ? capacidad : undefined
      })
      .subscribe({
        next: disponibilidad => {
          this.disponibilidad.set(disponibilidad);
          this.cargandoDisponibilidad.set(false);
        },
        error: error => this.fallo(error, this.errorDisponibilidad, this.cargandoDisponibilidad)
      });
  }

  protected comoHora(iso: string): string {
    return new Date(iso).toLocaleTimeString();
  }

  private cargarCatalogo(filter: ClassroomFilter): void {
    this.cargandoCatalogo.set(true);
    this.errorCatalogo.set(null);
    this.getAllClassrooms.execute(filter).subscribe({
      next: aulas => {
        this.catalogo.set(aulas);
        this.cargandoCatalogo.set(false);
      },
      error: error => this.fallo(error, this.errorCatalogo, this.cargandoCatalogo)
    });
  }

  private fallo(error: HttpErrorResponse, mensaje: WritableSignal<string | null>, cargando: WritableSignal<boolean>): void {
    cargando.set(false);
    if (error.status === 503) {
      mensaje.set(
        'No se puede confirmar la disponibilidad: la sincronización con SIGA está desactualizada. ' +
        'Solicita al personal administrativo lanzar una sincronización e intenta de nuevo.'
      );
    } else if (error.status === 401) {
      mensaje.set('Tu sesión expiró. Vuelve a iniciar sesión.');
    } else {
      mensaje.set(`No fue posible completar la consulta (HTTP ${(error as { status?: number }).status}).`);
    }
  }

  private valor(selector: string): string | null {
    const elemento = document.querySelector(selector) as HTMLInputElement | null;
    return elemento === null ? null : elemento.value;
  }

  private trasHoras(hora: number): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 1);
    const rellena = (valor: number) => String(valor).padStart(2, '0');
    return `${fecha.getFullYear()}-${rellena(fecha.getMonth() + 1)}-${rellena(fecha.getDate())}T${rellena(hora)}:00`;
  }
}
