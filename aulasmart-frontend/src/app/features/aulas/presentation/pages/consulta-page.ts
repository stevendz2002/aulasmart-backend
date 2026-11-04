import { Component, OnInit, ChangeDetectionStrategy, WritableSignal, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE, GET_CLASSROOM_OCCUPATION_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../application/use-cases/get-available-classrooms.use-case';
import { GetClassroomOccupationUseCase } from '../../application/use-cases/get-classroom-occupation.use-case';
import { CREATE_CLASSROOM_REQUEST_USE_CASE } from '../../../solicitudes/application/classroom-requests-di';
import { CreateClassroomRequestUseCase } from '../../../solicitudes/application/use-cases/create-classroom-request.use-case';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';
import { ClassroomOccupation } from '../../domain/entities/occupation';
import { ClassroomCard, ClassroomCardBuilder } from '../models/classroom-card';
import { ClassroomDetail, ClassroomDetailBuilder } from '../models/classroom-detail';
import { CreateClassroomRequestCommand } from '../../../solicitudes/domain/entities/classroom-request';

type PestanaEstadoValor = 'TODAS' | 'LIBRES' | 'OCUPADAS';

interface PestanaEstado {
  etiqueta: string;
  valor: PestanaEstadoValor;
  cuenta: number | null;
}

const MOTIVOS_GENERICOS: readonly string[] = [
  'Clase de recuperación',
  'Monitoría',
  'Defensa de proyecto',
  'Examen / evaluación',
  'Taller práctico',
  'Seminario / conferencia',
  'Otros'
];

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

    <!-- Barra de filtros: 6 campos en grid horizontal -->
    <section class="tarjeta filtros">
      <div class="filtros__grid">
        <label class="campo">
          Fecha
          <span class="input-icono">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 9h18M8 3v4M16 3v4"/></svg>
            <input type="date" [value]="fechaConsulta()" (change)="fechaConsulta.set($any($event.target).value)" />
          </span>
        </label>

        <label class="campo">
          Hora inicio
          <span class="input-icono">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            <input type="time" [value]="horaInicio()" (change)="horaInicio.set($any($event.target).value)" />
          </span>
        </label>

        <label class="campo">
          Hora fin
          <span class="input-icono">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            <input type="time" [value]="horaFin()" (change)="horaFin.set($any($event.target).value)" />
          </span>
        </label>

        <label class="campo">
          Bloque
          <span class="input-icono select">
            <select [value]="bloqueSeleccionado()" (change)="bloqueSeleccionado.set($any($event.target).value)">
              <option value="">Todos</option>
              @for (b of bloquesDisponibles(); track b) {
                <option [value]="b">{{ b }}</option>
              }
            </select>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
          </span>
        </label>

        <label class="campo">
          Capacidad mínima
          <input type="number" min="1" placeholder="Ej. 30" [value]="capacidad()" (input)="capacidad.set($any($event.target).value)" />
        </label>

        <label class="campo">
          Tipo de aula
          <span class="input-icono select">
            <select [value]="tipoAulaSeleccionado()" (change)="tipoAulaSeleccionado.set($any($event.target).value)">
              <option value="">Todos</option>
              @for (t of tiposDisponibles(); track t) {
                <option [value]="t">{{ t }}</option>
              }
            </select>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
          </span>
        </label>
      </div>

      <div class="filtros__acciones">
        <button type="button" class="boton" (click)="consultar()" [disabled]="cargandoDisponibilidad()">
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
              <div class="aula-card__meta">
                <span class="meta-item"><span class="meta-icono">👥</span>{{ card.capacity }}</span>
                <span class="meta-item"><span class="meta-icono">📍</span>{{ card.building }}</span>
              </div>
              <div class="chips">
                @for (c of card.characteristics; track c) {
                  <span class="chip caracteristica">{{ c }}</span>
                }
              </div>
              @if (card.status === 'OCUPADA' && liberacion(card.id); as hora) {
                <p class="liberacion">Se libera a las {{ hora }}</p>
              }
              <div class="aula-card__acciones">
                <button type="button" class="boton secundario" (click)="abrirDetalle(card)">Detalles</button>
                <button type="button" class="boton boton-solicitar" [disabled]="card.status !== 'LIBRE'" (click)="abrirSolicitudDirecta(card)">
                  Solicitar ↗
                </button>
              </div>
            </article>
          }
        }
      </div>
    </section>

    <!-- Modal 1: Ficha de detalles del aula -->
    @if (detalleConstruido(); as card) {
      <div class="modal-overlay" (click)="cerrarDetalle()">
        <div class="modal" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-labelledby="detalle-titulo">
          <header class="modal__cabecera">
            <div>
              <h3 id="detalle-titulo">{{ card.name }}</h3>
              <span class="chip estado-chip" [class.libre]="card.status === 'LIBRE'" [class.ocupada]="card.status === 'OCUPADA'">
                {{ card.statusLabel }}
              </span>
              <p class="modal__subtitulo">{{ card.subtitle }}</p>
            </div>
            <button type="button" class="modal__cerrar" (click)="cerrarDetalle()" aria-label="Cerrar">×</button>
          </header>

          <div class="modal__metricas">
            <div class="metrica">
              <span class="metrica__icono">👥</span>
              <span class="metrica__valor">{{ card.capacity }} personas</span>
            </div>
            <div class="metrica">
              <span class="metrica__icono">📍</span>
              <span class="metrica__valor">{{ card.building }}</span>
            </div>
            <div class="metrica">
              <span class="metrica__icono">🕐</span>
              <span class="metrica__valor">{{ card.freeNowLabel }}</span>
            </div>
          </div>

          <section class="modal__seccion">
            <h4>CARACTERÍSTICAS</h4>
            <div class="chips">
              @for (c of card.characteristics; track c) {
                <span class="chip caracteristica">{{ c }}</span>
              }
            </div>
          </section>

          <section class="modal__seccion">
            <h4>HORARIO DE HOY</h4>
            @if (cargandoOcupacion()) {
              <p class="mensaje-info">Consultando horario…</p>
            } @else if (timeline().length === 0) {
              <p class="mensaje-info">Sin franjas registradas para hoy.</p>
            } @else {
              <ul class="timeline">
                @for (fila of timeline(); track fila.start + fila.end) {
                  <li class="timeline__fila" [class.libre]="fila.free">
                    <span class="timeline__hora">{{ fila.start }} – {{ fila.end }}</span>
                    <span class="timeline__detalle" [class.libre]="fila.free">{{ fila.label }}</span>
                  </li>
                }
              </ul>
            }
          </section>

          <footer class="modal__acciones">
            <button type="button" class="boton secundario" (click)="cerrarDetalle()">Cerrar</button>
            <button type="button" class="boton boton-solicitar" [disabled]="card.status !== 'LIBRE'" (click)="abrirSolicitud()">Solicitar aula ↗</button>
          </footer>
        </div>
      </div>
    }

    <!-- Modal 2: Formulario de solicitud -->
    @if (modalSolicitudAbierto()) {
      <div class="modal-overlay" (click)="cerrarSolicitud()">
        <div class="modal" (click)="$event.stopPropagation()" role="dialog" aria-modal="true" aria-labelledby="solicitud-titulo">
          <header class="modal__cabecera">
            <div>
              <h3 id="solicitud-titulo">Solicitar aula {{ solicitudAula()?.name }}</h3>
              <p class="modal__subtitulo">Salón · Capacidad {{ solicitudAula()?.capacity }}</p>
            </div>
            <button type="button" class="modal__cerrar" (click)="cerrarSolicitud()" aria-label="Cerrar">×</button>
          </header>

          <form class="formulario" (submit)="enviarSolicitud($event)">
            <div class="fila-tiempo">
              <label>
                Fecha
                <input type="date" [value]="fechaSolicitud()" (change)="fechaSolicitud.set($any($event.target).value)" required />
              </label>
              <label>
                Inicio
                <input type="time" [value]="inicioSolicitud()" (change)="inicioSolicitud.set($any($event.target).value)" required />
              </label>
              <label>
                Fin
                <input type="time" [value]="finSolicitud()" (change)="finSolicitud.set($any($event.target).value)" required />
              </label>
            </div>

            <label>
              Asistentes
              <input type="number" min="1" [value]="asistentes()" (input)="asistentes.set($any($event.target).valueAsNumber)" />
            </label>

            <label>
              Motivo
              <select [value]="motivoSeleccionado()" (change)="motivoSeleccionado.set($any($event.target).value)">
                @for (m of motivosGenericos; track m) {
                  <option [value]="m">{{ m }}</option>
                }
              </select>
            </label>

            @if (motivoSeleccionado() === 'Otros') {
              <label>
                Describe el motivo
                <textarea rows="3" maxlength="300" placeholder="Ej. Clase de recuperación de Programación II"
                          [value]="motivoOtro()" (input)="motivoOtro.set($any($event.target).value)" required></textarea>
              </label>
            }

            <div class="banner-info">
              <span class="banner-info__icono">ℹ</span>
              <span>La solicitud no implica asignación inmediata. Será revisada por el área administrativa.</span>
            </div>

            @if (errorSolicitud(); as mensaje) {
              <p class="mensaje-error">{{ mensaje }}</p>
            }
            @if (exitoSolicitud(); as mensaje) {
              <p class="mensaje-exito">{{ mensaje }}</p>
            }

            <footer class="modal__acciones">
              <button type="button" class="boton secundario" (click)="cerrarSolicitud()">Cancelar</button>
              <button type="submit" class="boton" [disabled]="enviandoSolicitud()">
                {{ enviandoSolicitud() ? 'Enviando…' : 'Enviar solicitud' }}
              </button>
            </footer>
          </form>
        </div>
      </div>
    }
  `
})
export class ConsultaPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(GET_AVAILABLE_CLASSROOMS_USE_CASE);
  private readonly getOccupation = inject<GetClassroomOccupationUseCase>(GET_CLASSROOM_OCCUPATION_USE_CASE);
  private readonly createRequest = inject<CreateClassroomRequestUseCase>(CREATE_CLASSROOM_REQUEST_USE_CASE);

  protected readonly motivosGenericos = MOTIVOS_GENERICOS;

  // Filtros
  protected readonly fechaConsulta = signal<string>(this.fechaDeManana());
  protected readonly horaInicio = signal<string>('08:00');
  protected readonly horaFin = signal<string>('10:00');
  protected readonly bloqueSeleccionado = signal<string>('');
  protected readonly capacidad = signal<string>('');
  protected readonly tipoAulaSeleccionado = signal<string>('');

  protected readonly textoBusqueda = signal('');
  protected readonly pestanaActiva = signal<PestanaEstadoValor>('TODAS');

  protected readonly catalogo = signal<Classroom[] | null>(null);
  protected readonly cargandoCatalogo = signal(false);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly disponibilidad = signal<Availability | null>(null);
  protected readonly cargandoDisponibilidad = signal(false);
  protected readonly errorDisponibilidad = signal<string | null>(null);

  // Modal 1
  protected readonly detalle = signal<Classroom | null>(null);
  protected readonly cargandoOcupacion = signal(false);
  protected readonly ocupacion = signal<ClassroomOccupation[]>([]);
  protected readonly liberacionPorAula = signal<Map<number, string>>(new Map());

  protected liberacion(id: number): string | null {
    return this.liberacionPorAula().get(id) ?? null;
  }

  // Modal 2
  protected readonly modalSolicitudAbierto = signal(false);
  protected readonly solicitudAula = signal<Classroom | null>(null);
  protected readonly fechaSolicitud = signal<string>(this.fechaDeManana());
  protected readonly inicioSolicitud = signal<string>('08:00');
  protected readonly finSolicitud = signal<string>('10:00');
  protected readonly asistentes = signal<number>(30);
  protected readonly motivoSeleccionado = signal<string>(MOTIVOS_GENERICOS[0]);
  protected readonly motivoOtro = signal<string>('');
  protected readonly enviandoSolicitud = signal(false);
  protected readonly exitoSolicitud = signal<string | null>(null);
  protected readonly errorSolicitud = signal<string | null>(null);

  private readonly catalogoBase = computed<Classroom[] | null>(() => this.catalogo());

  private readonly bloquesSet = computed<Set<string>>(() => {
    const set = new Set<string>();
    for (const a of this.catalogoBase() ?? []) {
      set.add(a.building.name);
    }
    return set;
  });

  protected readonly bloquesDisponibles = computed<string[]>(() => [...this.bloquesSet()].sort());

  private readonly tiposSet = computed<Set<string>>(() => {
    const set = new Set<string>();
    for (const a of this.catalogoBase() ?? []) {
      set.add(a.type.name);
    }
    return set;
  });

  protected readonly tiposDisponibles = computed<string[]>(() => [...this.tiposSet()].sort());

  private readonly libresEnFranja = computed<Set<number> | null>(() => {
    const resultado = this.disponibilidad();
    return resultado === null ? null : new Set(resultado.classrooms.map(aula => aula.id));
  });

  private readonly aulasConEstado = computed<ClassroomCard[]>(() => {
    const catalogo = this.catalogoBase() ?? [];
    const libres = this.libresEnFranja();
    // filtrar por bloque y tipo seleccionados
    const bloque = this.bloqueSeleccionado();
    const tipo = this.tipoAulaSeleccionado();
    return catalogo
      .filter(a => bloque === '' || a.building.name === bloque)
      .filter(a => tipo === '' || a.type.name === tipo)
      .map(aula => {
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

  protected readonly detalleConstruido = computed<ClassroomDetail | null>(() => {
    const aula = this.detalle();
    if (aula === null) {
      return null;
    }
    const libres = this.libresEnFranja();
    const estado: 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO' =
      libres === null ? 'DESCONOCIDO' : libres.has(aula.id) ? 'LIBRE' : 'OCUPADA';
    return ClassroomDetailBuilder.of(aula, estado, this.ocupacion());
  });

  protected readonly timeline = computed(() => this.detalleConstruido()?.timeline ?? []);

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

  protected consultar(): void {
    const [anio, mes, dia] = this.fechaConsulta().split('-').map(Number);
    const [hI, mI] = this.horaInicio().split(':').map(Number);
    const [hF, mF] = this.horaFin().split(':').map(Number);
    const inicio = new Date(anio, mes - 1, dia, hI, mI);
    const fin = new Date(anio, mes - 1, dia, hF, mF);

    const capacidad = Number.parseInt(this.capacidad(), 10);
    this.obtenerDisponibilidad(inicio, fin, capacidad);
  }

  protected abrirDetalle(aula: ClassroomCard): void {
    const classroom = (this.catalogo() ?? []).find(c => c.id === aula.id);
    if (classroom === undefined) {
      return;
    }
    this.detalle.set(classroom);
    this.cargandoOcupacion.set(true);
    this.ocupacion.set([]);
    const hoy = new Date();
    const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate(), 0, 0);
    const fin = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate(), 23, 59);
    this.getOccupation.execute(classroom.id, { start: inicio, end: fin }).subscribe({
      next: ocupaciones => {
        this.ocupacion.set(ocupaciones);
        this.cargandoOcupacion.set(false);
        // Cache de hora de liberación para la tarjeta (primera franja que cubre la consulta).
        const liberacion = this.calcularLiberacion(ocupaciones, this.fechaConsulta(), this.horaInicio(), this.horaFin());
        if (liberacion !== null) {
          this.liberacionPorAula.update(m => {
            const siguiente = new Map(m);
            siguiente.set(classroom.id, liberacion);
            return siguiente;
          });
        }
      },
      error: () => {
        this.ocupacion.set([]);
        this.cargandoOcupacion.set(false);
      }
    });
  }

  protected cerrarDetalle(): void {
    this.detalle.set(null);
    this.ocupacion.set([]);
  }

  protected abrirSolicitudDirecta(aula: ClassroomCard): void {
    const classroom = (this.catalogo() ?? []).find(c => c.id === aula.id);
    if (classroom === undefined || aula.status !== 'LIBRE') {
      return;
    }
    this.solicitudAula.set(classroom);
    this.fechaSolicitud.set(this.fechaConsulta());
    this.inicioSolicitud.set(this.horaInicio());
    this.finSolicitud.set(this.horaFin());
    this.asistentes.set(30);
    this.motivoSeleccionado.set(MOTIVOS_GENERICOS[0]);
    this.motivoOtro.set('');
    this.errorSolicitud.set(null);
    this.exitoSolicitud.set(null);
    this.modalSolicitudAbierto.set(true);
  }

  protected abrirSolicitud(): void {
    const aula = this.detalle();
    if (aula === null) {
      return;
    }
    this.solicitudAula.set(aula);
    this.fechaSolicitud.set(this.fechaConsulta());
    this.inicioSolicitud.set(this.horaInicio());
    this.finSolicitud.set(this.horaFin());
    this.asistentes.set(30);
    this.motivoSeleccionado.set(MOTIVOS_GENERICOS[0]);
    this.motivoOtro.set('');
    this.errorSolicitud.set(null);
    this.exitoSolicitud.set(null);
    this.modalSolicitudAbierto.set(true);
  }

  protected cerrarSolicitud(): void {
    this.modalSolicitudAbierto.set(false);
    this.solicitudAula.set(null);
  }

  protected enviarSolicitud(evento: Event): void {
    evento.preventDefault();
    const aula = this.solicitudAula();
    if (aula === null) {
      return;
    }
    const motivo = this.motivoSeleccionado() === 'Otros'
      ? this.motivoOtro().trim()
      : this.motivoSeleccionado();
    if (motivo === '') {
      this.errorSolicitud.set('Describe el motivo de la solicitud.');
      return;
    }

    const [anio, mes, dia] = this.fechaSolicitud().split('-').map(Number);
    const [hI, mI] = this.inicioSolicitud().split(':').map(Number);
    const [hF, mF] = this.finSolicitud().split(':').map(Number);
    const inicio = new Date(anio, mes - 1, dia, hI, mI);
    const fin = new Date(anio, mes - 1, dia, hF, mF);
    if (!(fin.getTime() > inicio.getTime())) {
      this.errorSolicitud.set('La hora de fin debe ser posterior a la de inicio.');
      return;
    }

    const command: CreateClassroomRequestCommand = {
      classroomId: aula.id,
      start: inicio,
      end: fin,
      reason: motivo
    };

    this.enviandoSolicitud.set(true);
    this.errorSolicitud.set(null);
    this.exitoSolicitud.set(null);
    this.createRequest.execute(command).subscribe({
      next: () => {
        this.enviandoSolicitud.set(false);
        this.exitoSolicitud.set('Solicitud enviada. Será revisada por el área administrativa.');
        setTimeout(() => {
          this.modalSolicitudAbierto.set(false);
          this.detalle.set(null);
          this.ocupacion.set([]);
          this.solicitudAula.set(null);
          this.exitoSolicitud.set(null);
        }, 1500);
      },
      error: (err: HttpErrorResponse) => {
        this.enviandoSolicitud.set(false);
        const detalle = typeof err.error?.message === 'string' ? err.error.message : null;
        this.errorSolicitud.set(
          err.status === 409
            ? (detalle ?? 'La franja solicitada se solapa con una reserva aprobada.')
            : err.status === 400
              ? (detalle ?? 'Datos inválidos: revisa fecha, horas y motivo.')
              : `No fue posible enviar la solicitud (HTTP ${err.status}).`
        );
      }
    });
  }

  private calcularLiberacion(ocupaciones: ClassroomOccupation[], fecha: string, horaInicio: string, horaFin: string): string | null {
    const [anio, mes, dia] = fecha.split('-').map(Number);
    const [hI, mI] = horaInicio.split(':').map(Number);
    const [hF, mF] = horaFin.split(':').map(Number);
    const consultaInicio = new Date(anio, mes - 1, dia, hI, mI).getTime();
    const consultaFin = new Date(anio, mes - 1, dia, hF, mF).getTime();
    const solapada = ocupaciones.find(o => {
      const i = new Date(o.start).getTime();
      const f = new Date(o.end).getTime();
      return i < consultaFin && f > consultaInicio;
    });
    if (solapada === undefined) {
      return null;
    }
    const fin = new Date(solapada.end);
    return `${String(fin.getHours()).padStart(2, '0')}:${String(fin.getMinutes()).padStart(2, '0')}`;
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
