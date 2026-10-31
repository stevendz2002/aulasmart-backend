import { Component, OnInit, WritableSignal, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AulasData } from './aulas-data';
import { Aula, Disponibilidad } from '../../core/modelos';

@Component({
  selector: 'app-aulas-page',
  template: `
    <section class="seccion">
      <h2>Catálogo de aulas</h2>
      <p class="ayuda">Catálogo sincronizado desde SIGA. Busca por nombre y filtra por capacidad mínima.</p>

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
                <td>{{ aula.nombre }}</td>
                <td>{{ aula.edificio.nombre }}</td>
                <td>{{ aula.tipoAula.nombre }}</td>
                <td>{{ aula.dependencia.nombre }}</td>
                <td class="numero">{{ aula.capacidad }}</td>
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
              <th>Capacidad</th>
            </tr>
          </thead>
          <tbody>
            @for (aula of resultado.aulas; track aula.id) {
              <tr>
                <td>{{ aula.nombre }}</td>
                <td>{{ aula.edificio.nombre }}</td>
                <td class="numero">{{ aula.capacidad }}</td>
              </tr>
            }
          </tbody>
        </table>
        <p class="mensaje-info">
          {{ resultado.aulas.length }} aula(s) libres en el intervalo · verificado a las
          {{ comoHora(resultado.verificadoEn) }}.
        </p>
      }
    </section>
  `
})
export class AulasPage implements OnInit {
  private readonly aulasData = inject(AulasData);

  private readonly catalogo = signal<Aula[] | null>(null);
  private readonly nombreBuscado = signal('');

  protected readonly aulas = computed<Aula[] | null>(() => {
    const todas = this.catalogo();
    if (todas === null) {
      return null;
    }
    const nombre = this.nombreBuscado().toLowerCase();
    return nombre === '' ? todas : todas.filter(aula => aula.nombre.toLowerCase().includes(nombre));
  });

  protected readonly cargandoCatalogo = signal(false);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly disponibilidad = signal<Disponibilidad | null>(null);
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
    this.cargarCatalogo({
      capacidadMinima: Number.isFinite(capacidad) && capacidad > 0 ? capacidad : undefined
    });
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
    this.aulasData
      .disponibles(
        new Date(inicio),
        new Date(fin),
        { capacidadMinima: Number.isFinite(capacidad) && capacidad > 0 ? capacidad : undefined }
      )
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

  private cargarCatalogo(filtro: { capacidadMinima?: number }): void {
    this.cargandoCatalogo.set(true);
    this.errorCatalogo.set(null);
    this.aulasData.listar(filtro).subscribe({
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
      mensaje.set(`No fue posible completar la consulta (HTTP ${error.status}).`);
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
