import { Component, OnInit, ChangeDetectionStrategy, inject, signal } from '@angular/core';

import { GET_ALL_CLASSROOMS_USE_CASE, TRIGGER_SIGA_SYNC_USE_CASE } from '../../application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../application/use-cases/get-all-classrooms.use-case';
import { TriggerSigaSyncUseCase } from '../../application/use-cases/trigger-siga-sync.use-case';
import { Classroom } from '../../domain/entities/classroom';
import { SigaSyncRun } from '../../domain/entities/siga-sync-run';

/**
 * Vista de gestión del catálogo de aulas, exclusiva de
 * ADMINISTRATIVO / ADMINISTRADOR. El CRUD de aulas depende de endpoints de
 * escritura que el backend aún no publica (solo lectura); la sincronización
 * SIGA sí es un endpoint real y se ofrece a este mismo rol.
 */
@Component({
  selector: 'app-gestion-aulas-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="seccion">
      <h2>Gestión de aulas</h2>
      <p class="ayuda">
        Catálogo sincronizado desde SIGA. Vista de solo lectura.
        <button type="button" class="boton secundario" (click)="recargar()" [disabled]="cargando()">
          {{ cargando() ? 'Consultando…' : 'Actualizar' }}
        </button>
        <button type="button" class="boton" (click)="sincronizar('AULAS')" [disabled]="sincronizando()">
          {{ sincronizando() ? 'Sincronizando…' : 'Sincronizar catálogo' }}
        </button>
        <button type="button" class="boton" (click)="sincronizar('EVENTOS')" [disabled]="sincronizando()">
          Sincronizar eventos
        </button>
      </p>

      <p class="mensaje-info">
        «Sincronizar catálogo» refresca las aulas; «Sincronizar eventos» descarga la ocupación real
        por clases del calendario público de SIGA (próximos 7 días) para que la consulta de
        disponibilidad excluya también esos salones ocupados.
      </p>

      @if (error(); as mensaje) {
        <p class="mensaje-error">{{ mensaje }}</p>
      }

      @if (ultimaSincronizacion(); as corrida) {
        <p class="mensaje-exito">
          Sincronización {{ corrida.status.toLowerCase() }} de {{ corrida.resource }}:
          {{ corrida.recordsProcessed }} registro(s) en {{ (corrida.durationMilliseconds / 1000).toFixed(1) }} s.
        </p>
      }
      @if (errorSincronizacion(); as mensaje) {
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
  private readonly triggerSigaSync = inject<TriggerSigaSyncUseCase>(TRIGGER_SIGA_SYNC_USE_CASE);

  protected readonly aulas = signal<Classroom[] | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly sincronizando = signal(false);
  protected readonly ultimaSincronizacion = signal<SigaSyncRun | null>(null);
  protected readonly errorSincronizacion = signal<string | null>(null);

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

  protected sincronizar(recurso: 'AULAS' | 'EVENTOS'): void {
    this.sincronizando.set(true);
    this.errorSincronizacion.set(null);
    this.ultimaSincronizacion.set(null);
    this.triggerSigaSync.execute(recurso).subscribe({
      next: corridas => {
        this.sincronizando.set(false);
        this.ultimaSincronizacion.set(corridas[0] ?? null);
        this.recargar();
      },
      error: error => {
        this.sincronizando.set(false);
        this.errorSincronizacion.set(
          error.status === 403
            ? 'Tu rol no permite lanzar la sincronización.'
            : `No fue posible lanzar la sincronización (HTTP ${error.status ?? '—'}).`
        );
      }
    });
  }
}
