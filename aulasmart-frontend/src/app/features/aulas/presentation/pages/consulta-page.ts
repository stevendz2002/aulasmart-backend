import { Component, OnInit, ChangeDetectionStrategy, WritableSignal, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../application/use-cases/get-available-classrooms.use-case';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';
import { DAY_TIME_SLOTS, DEFAULT_TIME_SLOT_ID, TimeSlot } from '../../domain/entities/time-slot';
import { ClassroomCard, ClassroomCardBuilder } from '../models/classroom-card';

type PestanaEstadoValor = 'TODAS' | 'LIBRES' | 'OCUPADAS';

interface PestanaEstado {
  etiqueta: string;
  valor: PestanaEstadoValor;
  cuenta: number | null;
}

/**
 * Consulta de disponibilidad rediseñada: panel de franja con chips horarios,
 * grid de tarjetas por aula (patrón Builder para la vista) y filtros
 * descriptivos. La ocupación real proviene de SIGA (clases) + reservas
 * APROBADAS de AulaSmart.
 */
@Component({
  selector: 'app-consulta-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './consulta-page.css',
  template: `
    <section class="cabecera">
      <h2>Consulta de aulas</h2>
      <p class="ayuda">
        Explora el catálogo sincronizado desde SIGA y verifica qué aulas están libres
        en la franja de tu clase. Un aula aparece ocupada si tiene clase en el
        calendario académico de SIGA o una reserva APROBADA en AulaSmart.
      </p>
    </section>

    <section class="tarjeta consulta">
      <div class="consulta__franja">
        <label class="campo-fecha">
          Fecha
          <input id="fechaConsulta" type="date" [value]="fechaConsulta()"
                 (change)="cambiarFecha($any($event.target).value)" />
        </label>

        <div class="chips-franja" role="group" aria-label="Franjas horarias">
          @for (franja of franjas; track franja.id) {
            <button type="button" class="chip-franja"
                    [class.activa]="franjaSeleccionada() === franja.id"
                    (click)="seleccionarFranja(franja)">
              {{ franja.label }}
            </button>
          }
        </div>

        <label class="campo-capacidad">
          Capacidad mínima (opcional)
          <input id="capacidadDisponibilidad" type="number" min="1"
                 [value]="capacidad()" (input)="capacidad.set($any($event.target).value)" />
        </label>

        <button type="button" class="boton" (click)="consultar()" [disabled]="cargandoDisponibilidad() || franjaSeleccionada() === null">
          {{ cargandoDisponibilidad() ? 'Consultando…' : 'Consultar disponibilidad' }}
        </button>
      </div>

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
    </section>

    <section class="tarjeta">
      <div class="controles">
        <div class="pestanas" role="tablist" aria-label="Filtrar por disponibilidad en la franja">
          @for (pestana of pestanas(); track pestana.valor) {
            <button type="button" role="tab" [attr.aria-selected]="pestanaActiva() === pestana.valor"
                    [class.activa]="pestanaActiva() === pestana.valor" (click)="elegirPestana(pestana.valor)">
              {{ pestana.etiqueta }} <span class="cuenta">{{ pestana.cuenta ?? '—' }}</span>
            </button>
          }
        </div>

        <label class="busqueda">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" aria-hidden="true">
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

      <div class="grid-aulas">
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
          @for (card of visibles(); track card.id) {
            <article class="aula-card" [class.libre]="card.status === 'LIBRE'" [class.ocupada]="card.status === 'OCUPADA'">
              <header class="aula-card__cabecera">
                <h3>{{ card.name }}</h3>
                <span class="chip estado-chip" [class.libre]="card.status === 'LIBRE'" [class.ocupada]="card.status === 'OCUPADA'">
                  {{ card.statusLabel }}
                </span>
              </header>
              <p class="aula-card__ubicacion">{{ card.building }} · {{ card.type }}</p>
              <footer class="aula-card__pie">
                <span class="aula-card__departamento">{{ card.department }}</span>
                <span class="aula-card__capacidad">{{ card.capacity }} sillas</span>
              </footer>
            </article>
          }
        }
      </div>
    </section>
  `
})
export class ConsultaPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(GET_AVAILABLE_CLASSROOMS_USE_CASE);

  protected readonly franjas = DAY_TIME_SLOTS;
  protected readonly fechaConsulta = signal<string>(this.fechaDeManana());
  protected readonly franjaSeleccionada = signal<string | null>(DEFAULT_TIME_SLOT_ID);
  protected readonly capacidad = signal<string>('');

  protected readonly textoBusqueda = signal('');
  protected readonly pestanaActiva = signal<PestanaEstadoValor>('TODAS');

  protected readonly catalogo = signal<Classroom[] | null>(null);
  protected readonly cargandoCatalogo = signal(false);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly disponibilidad = signal<Availability | null>(null);
  protected readonly cargandoDisponibilidad = signal(false);
  protected readonly errorDisponibilidad = signal<string | null>(null);

  /** Alias del catálogo usado por las vistas derivadas. */
  private readonly catalogoBase = computed<Classroom[] | null>(() => this.catalogo());

  /** Ids libres según la última franja consultada (dato real de la API). */
  private readonly libresEnFranja = computed<Set<number> | null>(() => {
    const resultado = this.disponibilidad();
    return resultado === null ? null : new Set(resultado.classrooms.map(aula => aula.id));
  });

  /** Tarjetas construidas con el patrón Builder (aula + estado de la franja). */
  private readonly aulasConEstado = computed<ClassroomCard[]>(() => {
    const catalogo = this.catalogoBase() ?? [];
    const libres = this.libresEnFranja();
    return catalogo.map(aula => {
      const estado: 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO' =
        libres === null ? 'DESCONOCIDO' : libres.has(aula.id) ? 'LIBRE' : 'OCUPADA';
      return ClassroomCardBuilder.of(aula, estado);
    });
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
      { etiqueta: 'Libres', valor: 'LIBRES', cuenta: filas.filter(f => f.status === 'LIBRE').length },
      { etiqueta: 'Ocupadas', valor: 'OCUPADAS', cuenta: filas.filter(f => f.status === 'OCUPADA').length }
    ];
  });

  /** Filtrado cliente por pestaña y texto (nombre, edificio, tipo o dependencia). */
  protected readonly visibles = computed<ClassroomCard[]>(() => {
    const pestana = this.pestanaActiva();
    const texto = this.textoBusqueda().trim().toLowerCase();
    return this.aulasConEstado().filter(card => {
      if (pestana === 'LIBRES' && card.status !== 'LIBRE') {
        return false;
      }
      if (pestana === 'OCUPADAS' && card.status !== 'OCUPADA') {
        return false;
      }
      if (texto === '') {
        return true;
      }
      return (
        card.name.toLowerCase().includes(texto) ||
        card.building.toLowerCase().includes(texto) ||
        card.type.toLowerCase().includes(texto) ||
        card.department.toLowerCase().includes(texto)
      );
    });
  });

  ngOnInit(): void {
    const nombre = (this.route.snapshot.queryParamMap.get('name') ?? '').trim();
    this.textoBusqueda.set(nombre);
    this.cargarCatalogo({});
    this.consultar();
  }

  protected elegirPestana(valor: PestanaEstadoValor): void {
    this.pestanaActiva.set(valor);
  }

  protected buscar(texto: string): void {
    this.textoBusqueda.set(texto);
  }

  protected hayaDisponibilidad(): boolean {
    return this.libresEnFranja() !== null;
  }

  protected cambiarFecha(valor: string): void {
    this.fechaConsulta.set(valor);
    if (this.franjaSeleccionada() !== null) {
      this.consultar();
    }
  }

  protected seleccionarFranja(franja: TimeSlot): void {
    this.franjaSeleccionada.set(franja.id);
    this.consultar();
  }

  protected consultar(): void {
    const franja = this.franjas.find(f => f.id === this.franjaSeleccionada());
    if (franja === null || franja === undefined) {
      this.errorDisponibilidad.set('Selecciona una franja horaria.');
      return;
    }
    const [anio, mes, dia] = this.fechaConsulta().split('-').map(Number);
    const [hI, mI] = franja.start.split(':').map(Number);
    const [hF, mF] = franja.end.split(':').map(Number);
    const inicio = new Date(anio, mes - 1, dia, hI, mI);
    const fin = new Date(anio, mes - 1, dia, hF, mF);

    const capacidad = Number.parseInt(this.capacidad(), 10);
    this.obtenerDisponibilidad(inicio, fin, capacidad);
  }

  protected fechaCorta(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' });
  }

  protected comoHora(iso: string): string {
    return new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
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

  /** Consulta automática de la franja por defecto al abrir la página. */
  private consultarFranjaTotal(): void {
    // La consulta inicial usa la franja por defecto; se dispara desde ngOnInit.
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

  private fechaDeManana(): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 1);
    const rellena = (valor: number) => String(valor).padStart(2, '0');
    return `${fecha.getFullYear()}-${rellena(fecha.getMonth() + 1)}-${rellena(fecha.getDate())}`;
  }
}
