import { Component, OnInit, ChangeDetectionStrategy, WritableSignal, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../application/use-cases/get-available-classrooms.use-case';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';

type EstadoFranja = 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO';

interface AulaVista {
  aula: Classroom;
  estado: EstadoFranja;
}

interface PestanaEstado {
  etiqueta: string;
  valor: 'TODAS' | 'LIBRES' | 'OCUPADAS';
  cuenta: number | null;
}

/**
 * Consulta de aulas (diseño aprobado): formulario de franja contra el
 * endpoint real de disponibilidad, pestanas Todas / Libres / Ocupadas con
 * conteos reales, buscador del lado del cliente y chips de estado derivados
 * de la respuesta de la API (nunca datos simulados). El catálogo llega
 * sincronizado desde SIGA.
 */
@Component({
  selector: 'app-consulta-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  styleUrl: './consulta-page.css',
  template: `
    <header class="cabecera">
      <div>
        <h2>Consulta de aulas</h2>
        <p class="ayuda">
          Explora el catálogo sincronizado desde SIGA y verifica qué aulas están libres
          en la franja de tu clase. Un aula aparece ocupada si tiene clase en el calendario
          académico de SIGA o una reserva APROBADA en AulaSmart.
        </p>
      </div>
    </header>

    @if (errorCatalogo(); as mensaje) {
      <p class="mensaje-error">{{ mensaje }}</p>
    }

    <section class="tarjeta">
      <form class="franja" (submit)="consultarDisponibilidad($event)">
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
          {{ cargandoDisponibilidad() ? 'Consultando…' : 'Consultar disponibilidad' }}
        </button>
      </form>

      @if (disponibilidad(); as resultado) {
        <p class="nota-franja">
          Franja [{{ fechaCorta(resultado.range.start) }} {{ comoHora(resultado.range.start) }} –
          {{ comoHora(resultado.range.end) }}) verificada a las {{ comoHora(resultado.range.verifiedAt) }}:
          {{ resultado.classrooms.length }} aula(s) libres.
        </p>
      }

      @if (errorDisponibilidad(); as mensaje) {
        <p class="mensaje-error">{{ mensaje }}</p>
      }

      <div class="controles">
        <div class="pestanas" role="tablist" aria-label="Filtrar por disponibilidad en la franja">
          @for (pestana of pestanas(); track pestana.valor) {
            <button
              type="button"
              role="tab"
              [class.activa]="pestanaActiva() === pestana.valor"
              [attr.aria-selected]="pestanaActiva() === pestana.valor"
              (click)="elegirPestana(pestana.valor)"
            >
              {{ pestana.etiqueta }} <span class="cuenta">{{ pestana.cuenta ?? '—' }}</span>
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
            placeholder="Buscar por nombre, edificio o tipo…"
            aria-label="Buscar aulas"
            [value]="textoBusqueda()"
            (input)="buscar(entradaBusqueda.value)"
          />
        </label>
      </div>

      <div class="tabla-envoltorio">
        @if (catalogo() === null || cargandoCatalogo() || cargandoDisponibilidad()) {
          <p class="mensaje-info">Consultando catálogo…</p>
        } @else if (visibles().length === 0) {
          <p class="mensaje-info">
            @if ((catalogo()?.length ?? 0) === 0) {
              El catálogo no tiene aulas todavía: el personal administrativo debe lanzar la sincronización con SIGA.
            } @else if (!hayaDisponibilidad()) {
              Ninguna aula coincide con la búsqueda. Consulta también la disponibilidad de la franja.
            } @else {
              Ninguna aula coincide con el filtro actual.
            }
          </p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Aula</th>
                <th>Edificio</th>
                <th>Tipo</th>
                <th>Dependencia</th>
                <th class="numero">Capacidad</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (vista of visibles(); track vista.aula.id) {
                <tr>
                  <td class="aula">{{ vista.aula.name }}</td>
                  <td>{{ vista.aula.building.name }}</td>
                  <td>{{ vista.aula.type.name }}</td>
                  <td>{{ vista.aula.department.name }}</td>
                  <td class="numero">{{ vista.aula.capacity }}</td>
                  <td>
                    <span [class]="estadoChip(vista.estado)">
                      {{ etiquetaEstado(vista.estado) }}
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
          <p class="mensaje-info">{{ visibles().length }} de {{ catalogo()?.length ?? 0 }} aula(s) listadas · franja [inicio, fin).</p>
        }
      </div>
    </section>
  `
})
export class ConsultaPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(
    GET_AVAILABLE_CLASSROOMS_USE_CASE
  );

  protected readonly textoBusqueda = signal('');
  protected readonly pestanaActiva = signal<PestanaEstado['valor']>('TODAS');

  protected readonly catalogo = signal<Classroom[] | null>(null);
  protected readonly cargandoCatalogo = signal(false);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly disponibilidad = signal<Availability | null>(null);
  protected readonly cargandoDisponibilidad = signal(false);
  protected readonly errorDisponibilidad = signal<string | null>(null);

  protected readonly inicioPorDefecto = this.trasHoras(14);
  protected readonly finPorDefecto = this.trasHoras(16);

  /** Alias del catálogo usado por las vistas derivadas. */
  private readonly catalogoBase = computed<Classroom[] | null>(() => this.catalogo());

  /** Ids libres según la última franja consultada (dato real de la API). */
  private readonly libresEnFranja = computed<Set<number> | null>(() => {
    const resultado = this.disponibilidad();
    return resultado === null ? null : new Set(resultado.classrooms.map(aula => aula.id));
  });

  private readonly aulasConEstado = computed<AulaVista[]>(() => {
    const catalogo = this.catalogoBase() ?? [];
    const libres = this.libresEnFranja();
    return catalogo.map(aula => ({
      aula,
      estado: libres === null ? 'DESCONOCIDO' : libres.has(aula.id) ? 'LIBRE' : 'OCUPADA'
    }));
  });

  protected readonly pestanas = computed<PestanaEstado[]>(() => {
    const filas = this.aulasConEstado();
    const todas = filas.length;
    if (this.libresEnFranja() === null) {
      return [
        { etiqueta: 'Todas', valor: 'TODAS', cuenta: todas },
        { etiqueta: 'Libres', valor: 'LIBRES', cuenta: null },
        { etiqueta: 'Ocupadas', valor: 'OCUPADAS', cuenta: null }
      ];
    }
    return [
      { etiqueta: 'Todas', valor: 'TODAS', cuenta: todas },
      { etiqueta: 'Libres', valor: 'LIBRES', cuenta: filas.filter(f => f.estado === 'LIBRE').length },
      { etiqueta: 'Ocupadas', valor: 'OCUPADAS', cuenta: filas.filter(f => f.estado === 'OCUPADA').length }
    ];
  });

  /** Filtrado cliente por pestana y texto (nombre, edificio, tipo o dependencia). */
  protected readonly visibles = computed<AulaVista[]>(() => {
    const pestana = this.pestanaActiva();
    const texto = this.textoBusqueda().trim().toLowerCase();
    return this.aulasConEstado().filter(vista => {
      if (pestana === 'LIBRES' && vista.estado !== 'LIBRE') {
        return false;
      }
      if (pestana === 'OCUPADAS' && vista.estado !== 'OCUPADA') {
        return false;
      }
      if (texto === '') {
        return true;
      }
      const aula = vista.aula;
      return (
        aula.name.toLowerCase().includes(texto) ||
        aula.building.name.toLowerCase().includes(texto) ||
        aula.type.name.toLowerCase().includes(texto) ||
        aula.department.name.toLowerCase().includes(texto)
      );
    });
  });

  ngOnInit(): void {
    const nombre = (this.route.snapshot.queryParamMap.get('name') ?? '').trim();
    this.textoBusqueda.set(nombre);
    this.cargarCatalogo({});
    this.consultarFranjaTotal();
  }

  protected elegirPestana(valor: PestanaEstado['valor']): void {
    this.pestanaActiva.set(valor);
  }

  protected buscar(texto: string): void {
    this.textoBusqueda.set(texto);
  }

  protected hayaDisponibilidad(): boolean {
    return this.libresEnFranja() !== null;
  }

  protected estadoChip(estado: EstadoFranja): string {
    return `estado ${estado.toLowerCase()}`;
  }

  protected etiquetaEstado(estado: EstadoFranja): string {
    if (estado === 'LIBRE') {
      return 'Libre';
    }
    return estado === 'OCUPADA' ? 'Ocupada en la franja' : '—';
  }

  protected fechaCorta(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' });
  }

  protected comoHora(iso: string): string {
    return new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
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
    this.obtenerDisponibilidad(new Date(inicio), new Date(fin), capacidad);
  }

  /** Consulta automática de la franja por defecto al abrir la página. */
  private consultarFranjaTotal(): void {
    this.obtenerDisponibilidad(
      new Date(`${this.inicioPorDefecto}:00`),
      new Date(`${this.finPorDefecto}:00`),
      0
    );
  }

  private obtenerDisponibilidad(inicio: Date, fin: Date, capacidad: number): void {
    const filtro: ClassroomFilter = { minCapacity: Number.isFinite(capacidad) && capacidad > 0 ? capacidad : undefined };
    this.cargandoDisponibilidad.set(true);
    this.errorDisponibilidad.set(null);
    this.getAvailableClassrooms.execute(inicio, fin, filtro).subscribe({
      next: disponibilidad => {
        this.disponibilidad.set(disponibilidad);
        this.cargandoDisponibilidad.set(false);
        this.ajustarPestanaSegunDisponibilidad();
      },
      error: error => this.fallo(error, this.errorDisponibilidad, this.cargandoDisponibilidad)
    });
  }

  /** Con datos de franja, la pestana por omisión vuelve a «Todas». */
  private ajustarPestanaSegunDisponibilidad(): void {
    this.pestanaActiva.set('TODAS');
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
        'No se puede confirmar la disponibilidad: el catálogo o los eventos de SIGA están ' +
        'desactualizados. Solicita al personal administrativo lanzar la sincronización e intenta de nuevo.'
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
