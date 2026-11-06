import { Component, OnInit, ChangeDetectionStrategy, inject, signal } from '@angular/core';

import { GET_ALL_CLASSROOMS_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { Classroom } from '../../domain/entities/classroom';

/**
 * Vista de gestión del catálogo de aulas, exclusiva de
 * ADMINISTRATIVO / ADMINISTRADOR. Catálogo sincronizado automáticamente desde SIGA UCEVA.
 */
@Component({
  selector: 'app-gestion-aulas-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="seccion">
      <h2>Gestión de aulas</h2>
      <p class="ayuda">
        Catálogo sincronizado automáticamente desde SIGA UCEVA. Vista de solo lectura.
        <button type="button" class="boton secundario" (click)="recargar()" [disabled]="cargando()">
          {{ cargando() ? 'Consultando…' : 'Actualizar' }}
        </button>
      </p>

      @if (error(); as mensaje) {
        <p class="mensaje-error">{{ mensaje }}</p>
      }

      @if (aulas(); as lista) {
        <table>
          <thead>
            <tr>
              <th>Código SIGA</th>
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
                <td>{{ aula.externalCode }}</td>
                <td>{{ aula.name }}</td>
                <td>{{ aula.building.name }}</td>
                <td>{{ aula.type.name }}</td>
                <td>{{ aula.department.name }}</td>
                <td class="numero">{{ aula.capacity }}</td>
              </tr>
            }
          </tbody>
        </table>
        <p class="mensaje-info">{{ lista.length }} aula(s) en catálogo.</p>
      }
    </section>
  `
})
export class GestionAulasPage implements OnInit {
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);

  protected readonly aulas = signal<Classroom[] | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.recargar();
  }

  protected recargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.getAllClassrooms.execute({}).subscribe({
      next: aulas => {
        this.aulas.set(aulas);
        this.cargando.set(false);
      },
      error: error => {
        this.cargando.set(false);
        this.error.set(`No fue posible consultar el catálogo (HTTP ${error.status ?? '—'}).`);
      }
    });
  }
}
