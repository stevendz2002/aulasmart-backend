import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, OnInit, ChangeDetectionStrategy, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/auth-service';
import { nombreDesdeCorreo } from '../../../../core/nombre-usuario';
import {
  GET_ALL_CLASSROOMS_USE_CASE,
  GET_AVAILABLE_CLASSROOMS_USE_CASE
} from '../../../aulas/application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../../aulas/application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../../aulas/application/use-cases/get-available-classrooms.use-case';
import { Classroom } from '../../../aulas/domain/entities/classroom';
import { GET_CLASSROOM_REQUESTS_USE_CASE } from '../../../solicitudes/application/classroom-requests-di';
import { GetClassroomRequestsUseCase } from '../../../solicitudes/application/use-cases/get-classroom-requests.use-case';
import { ClassroomRequest } from '../../../solicitudes/domain/entities/classroom-request';

interface Indicador {
  titulo: string;
  valor: string;
  detalle: string;
  variante: 'libre' | 'catalogo' | 'pendiente' | 'aprobada';
}

interface UltimaSolicitud {
  id: number;
  aula: string;
  aulaCompleta: string;
  docente: string;
  momento: string;
  estado: string;
}

interface LibresPorEdificio {
  edificio: string;
  libres: number;
  total: number;
}

function saludoDelDia(ahora: Date): string {
  const hora = ahora.getHours();
  if (hora < 12) {
    return 'Buenos días';
  }
  return hora < 19 ? 'Buenas tardes' : 'Buenas noches';
}

function fechaLarga(ahora: Date): string {
  const texto = ahora.toLocaleDateString('es-CO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function horaCorta(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
}

/**
 * Dashboard (diseño aprobado) con datos exclusivamente reales de la API:
 * saludo contextual y aulas libres con franja de 2 h, cuatro indicadores
 * derivados de los endpoints existentes, últimas solicitudes de la sesión
 * de rol y libres por edificio. Los indicadores analíticos inventados del
 * boceto (ocupación semanal, ocupación por día y actividad reciente) no se
 * implementan: el backend no los expone y no se simulan.
 */
@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  styleUrl: './dashboard-page.css',
  template: `
    <section class="presentacion">
      <p class="fecha">{{ fechaHoy }}</p>
      <div class="fila">
        <div>
          <h1>{{ saludo }}, {{ nombreVisible() }}</h1>
          @if (mensajeDisponibilidad(); as mensaje) {
            <p class="lazo">{{ mensaje }}</p>
          } @else {
            <p class="lazo">
              Hay {{ libres() ?? '—' }} aula(s) libres en la próxima franja de 2 h
              ({{ rango() }}) en el campus.
            </p>
          }
        </div>
        <div class="acciones">
        <a class="boton secundario" routerLink="/consulta">Consultar aulas</a>
          <a class="boton" routerLink="/aulabot">Preguntar a AulaBot</a>
        </div>
      </div>
    </section>

    <section class="indicadores">
      @for (indicador of indicadores(); track indicador.titulo) {
        <article class="tarjeta" [class]="'var-' + indicador.variante">
          <header>
            <span class="titulo">{{ indicador.titulo }}</span>
            <span class="icono">●</span>
          </header>
          <span class="valor">{{ indicador.valor }}</span>
          <span class="detalle">{{ indicador.detalle }}</span>
        </article>
      }
    </section>

    <section class="tablero">
      <article class="panel solicitudes">
        <header>
          <div>
            <h3>Últimas solicitudes</h3>
            <span class="apagada">Movimiento reciente de solicitudes de aula</span>
          </div>
          <a routerLink="/solicitudes" [title]="'Ver todas las solicitudes'">Ver todas →</a>
        </header>

        @if (errorSolicitudes(); as mensaje) {
          <p class="mensaje-error">{{ mensaje }}</p>
        } @else if (ultimas().length === 0) {
          <p class="mensaje-info">Sin solicitudes registradas todavía.</p>
        } @else {
          <ul class="listado">
            @for (solicitud of ultimas(); track solicitud.id) {
              <li>
                <span class="aula" [title]="solicitud.aulaCompleta">{{ solicitud.aula }}</span>
                <div class="contenido">
                  <strong>{{ solicitud.docente }}</strong>
                  <span>{{ solicitud.momento }}</span>
                </div>
                <span class="estado {{ solicitud.estado }}">{{ solicitud.estado }}</span>
              </li>
            }
          </ul>
        }
      </article>

      <article class="panel edificios">
        <header>
          <div>
            <h3>Libres ahora por edificio</h3>
            <span class="apagada">Aulas libres en la franja {{ rango() }}</span>
          </div>
        </header>

        @if (mensajeDisponibilidad(); as mensaje) {
          <p class="mensaje-error">{{ mensaje }}</p>
        } @else if (porEdificio().length === 0) {
          <p class="mensaje-info">Sin datos de disponibilidad todavía.</p>
        } @else {
          <ul class="listado">
            @for (grupo of porEdificio(); track grupo.edificio) {
              <li>
                <span class="nombre">{{ grupo.edificio }}</span>
                <span class="barra">
                  <span class="relleno" [style.width.%]="(grupo.libres / grupo.total) * 100"></span>
                </span>
                <span class="cifra">{{ grupo.libres }} de {{ grupo.total }}</span>
              </li>
            }
          </ul>
        }
      </article>
    </section>

    <section class="panel aulabot">
      <div class="texto">
        <h3>Preguntar a AulaBot</h3>
        <p class="apagada">
          Asistente para consultas en lenguaje natural. El módulo chatbot del backend aún no está
          publicado; la vista de AulaBot documenta esa dependencia en lugar de simular respuestas.
        </p>
      </div>
      <a class="boton" routerLink="/aulabot">Abrir AulaBot</a>
    </section>
  `
})
export class DashboardPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private disponibilidadEnCurso = false;
  private readonly authService = inject(AuthService);
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(
    GET_AVAILABLE_CLASSROOMS_USE_CASE
  );
  private readonly getRequests = inject<GetClassroomRequestsUseCase>(GET_CLASSROOM_REQUESTS_USE_CASE);

  private readonly ahora = new Date();
  protected readonly saludo = saludoDelDia(this.ahora);
  protected readonly fechaHoy = fechaLarga(this.ahora);
  protected readonly nombreVisible = computed(() => {
    const sesion = this.authService.sesion();
    if (sesion === null) {
      return '';
    }
    return sesion.nombre && sesion.nombre.trim() !== ''
      ? sesion.nombre.trim()
      : nombreDesdeCorreo(sesion.correo);
  });

  private readonly catalogo = signal<Classroom[]>([]);
  private readonly solicitudes = signal<ClassroomRequest[] | null>(null);
  private readonly libresCampus = signal<Classroom[]>([]);

  protected readonly libres = signal<number | null>(null);
  protected readonly mensajeDisponibilidad = signal<string | null>(null);

  protected readonly pendientes = signal<number | null>(null);
  protected readonly aprobadas = signal<number | null>(null);
  protected readonly errorSolicitudes = signal<string | null>(null);

  private inicioFranja: Date = new Date();
  private finFranja: Date = new Date();

  ngOnInit(): void {
    this.inicioFranja = new Date();
    this.finFranja = new Date(this.ahora.getTime() + 2 * 60 * 60 * 1000);

    this.getAllClassrooms.execute({}).subscribe({
      next: aulas => this.catalogo.set(aulas),
      error: () => this.catalogo.set([])
    });

    this.actualizarDisponibilidad();
    interval(30000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.actualizarDisponibilidad());

    this.getRequests.execute('ALL').subscribe({
      next: solicitudes => {
        this.solicitudes.set(solicitudes);
        this.pendientes.set(solicitudes.filter(s => s.status === 'PENDIENTE').length);
        this.aprobadas.set(solicitudes.filter(s => s.status === 'APROBADA').length);
      },
      error: error =>
        this.errorSolicitudes.set(`Solicitudes no consultables ahora (HTTP ${error.status ?? '—'}).`)
    });
  }

  private actualizarDisponibilidad(): void {
    if (this.disponibilidadEnCurso) return;
    this.disponibilidadEnCurso = true;
    this.inicioFranja = new Date();
    this.finFranja = new Date(this.inicioFranja.getTime() + 2 * 60 * 60 * 1000);
    this.getAvailableClassrooms.execute(this.inicioFranja, this.finFranja, {})
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: disponibilidad => {
          this.disponibilidadEnCurso = false;
          this.libres.set(disponibilidad.classrooms.length);
          this.libresCampus.set(disponibilidad.classrooms);
          this.mensajeDisponibilidad.set(null);
        },
        error: error => {
          this.disponibilidadEnCurso = false;
          this.libres.set(null);
          this.libresCampus.set([]);
          this.mensajeDisponibilidad.set(error.status === 503
            ? 'La disponibilidad no se puede verificar: SIGA puede estar sincronizándose, haber fallado o no cubrir esta franja. Volveremos a consultar automáticamente.'
            : 'No fue posible consultar la disponibilidad. Volveremos a intentarlo automáticamente.');
        }
      });
  }

  protected rango(): string {
    return `${horaCorta(this.inicioFranja.toISOString())}–${horaCorta(this.finFranja.toISOString())}`;
  }

  protected readonly indicadores = computed<Indicador[]>(() => {
    const catalogo = this.catalogo();
    const total = catalogo.length;
    const bloques = new Set(catalogo.map(aula => aula.building.id)).size;
    return [
      {
        titulo: 'Aulas disponibles',
        valor: `${this.libres() ?? '—'}`,
        detalle: total > 0 ? `de ${total} en catálogo · franja 2 h` : 'catálogo no disponible',
        variante: 'libre'
      },
      {
        titulo: 'Aulas registradas',
        valor: `${total}`,
        detalle: bloques > 0 ? `en ${bloques} edificio(s)` : 'sincronizadas desde SIGA',
        variante: 'catalogo'
      },
      {
        titulo: 'Solicitudes pendientes',
        valor: `${this.pendientes() ?? '—'}`,
        detalle: 'esperando revisión (según tu rol)',
        variante: 'pendiente'
      },
      {
        titulo: 'Solicitudes aprobadas',
        valor: `${this.aprobadas() ?? '—'}`,
        detalle: 'visibles para tu rol',
        variante: 'aprobada'
      }
    ];
  });

  protected readonly ultimas = computed<UltimaSolicitud[]>(() => {
    const solicitudes = this.solicitudes();
    if (solicitudes === null) {
      return [];
    }
    const recientes = [...solicitudes].sort((a, b) => this.instante(b) - this.instante(a));
    return recientes.slice(0, 5).map(solicitud => {
      const aula = this.catalogo().find(candidata => candidata.id === solicitud.classroomId);
      const aulaCompleta = aula ? aula.name : `aula ${solicitud.classroomId}`;
      return {
        id: solicitud.id,
        aula: this.aulaCompacta(aulaCompleta),
        aulaCompleta,
        docente: solicitud.reason ? solicitud.reason : `Solicitud #${solicitud.id}`,
        momento: `Docente id ${solicitud.requesterId} · ${new Date(solicitud.start).toLocaleDateString('es-CO')} · ${horaCorta(solicitud.start)}–${horaCorta(solicitud.end)}`,
        estado: solicitud.status
      };
    });
  });

  protected readonly porEdificio = computed<LibresPorEdificio[]>(() => {
    const catalogo = this.catalogo();
    if (catalogo.length === 0) {
      return [];
    }
    const porId = new Map<number, string>();
    const totales = new Map<number, number>();
    for (const aula of catalogo) {
      porId.set(aula.building.id, aula.building.name);
      totales.set(aula.building.id, (totales.get(aula.building.id) ?? 0) + 1);
    }

    const libres = new Map<number, number>();
    for (const aula of this.libresCampus()) {
      libres.set(aula.building.id, (libres.get(aula.building.id) ?? 0) + 1);
    }
    return [...porId.entries()]
      .map(([edificioId, edificio]) => ({
        edificio,
        libres: libres.get(edificioId) ?? 0,
        total: totales.get(edificioId) ?? 0
      }))
      .sort((a, b) => b.libres - a.libres);
  });

  private instante(solicitud: ClassroomRequest): number {
    const tiempo = new Date(solicitud.createdAt).getTime();
    return Number.isNaN(tiempo) ? 0 : tiempo;
  }

  private aulaCompacta(nombre: string): string {
    const primera = nombre.split(' ')[0] ?? nombre;
    return primera.length > 12 ? `${primera.slice(0, 11)}…` : primera;
  }
}
