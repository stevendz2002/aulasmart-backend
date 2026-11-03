import { Component, OnInit, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ACADEMIC_PERIOD } from '../../../../core/config/academic-period';
import {
  GET_ALL_CLASSROOMS_USE_CASE,
  GET_AVAILABLE_CLASSROOMS_USE_CASE
} from '../../../aulas/application/classrooms-di';
import { GetAllClassroomsUseCase } from '../../../aulas/application/use-cases/get-all-classrooms.use-case';
import { GetAvailableClassroomsUseCase } from '../../../aulas/application/use-cases/get-available-classrooms.use-case';
import { GET_CLASSROOM_REQUESTS_USE_CASE } from '../../../solicitudes/application/classroom-requests-di';
import { GetClassroomRequestsUseCase } from '../../../solicitudes/application/use-cases/get-classroom-requests.use-case';

/**
 * Dashboard con indicadores calculados en vivo contra la API real
 * (sin datos simulados): catálogo total, aulas libres en la franja actual y
 * solicitudes pendientes visibles para el rol de la sesión.
 */
@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="seccion">
      <h2>Dashboard</h2>
      <p class="ayuda">
        Indicadores del período académico {{ periodo.label }}, calculados en vivo contra la API de AulaSmart.
        <button type="button" class="boton secundario" (click)="cargar()" [disabled]="cargando()">
          {{ cargando() ? 'Actualizando…' : 'Actualizar' }}
        </button>
      </p>

      <div class="tarjetas">
        <div class="tarjeta">
          <span class="etiqueta">Aulas en catálogo</span>
          @if (totalAulas() === null) {
            <span class="valor">—</span>
          } @else {
            <span class="valor">{{ totalAulas() }}</span>
          }
          <span class="detalle">Sincronizadas desde SIGA · <a routerLink="/consulta">ver consulta</a></span>
          @if (errorCatalogo(); as mensaje) {
            <span class="detalle error">{{ mensaje }}</span>
          }
        </div>

        <div class="tarjeta">
          <span class="etiqueta">Disponibles ahora</span>
          @if (libresAhora() === null) {
            <span class="valor">—</span>
          } @else {
            <span class="valor">{{ libresAhora() }}</span>
          }
          <span class="detalle">Franja {{ rangoAhora() }} · <a routerLink="/consulta">ver disponibles</a></span>
          @if (mensajeDisponibilidad(); as mensaje) {
            <span class="detalle error">{{ mensaje }}</span>
          }
        </div>

        <div class="tarjeta">
          <span class="etiqueta">Solicitudes pendientes</span>
          @if (pendientes() === null) {
            <span class="valor">—</span>
          } @else {
            <span class="valor">{{ pendientes() }}</span>
          }
          <span class="detalle">Visibles según tu rol · <a routerLink="/solicitudes">ver solicitudes</a></span>
          @if (errorSolicitudes(); as mensaje) {
            <span class="detalle error">{{ mensaje }}</span>
          }
        </div>
      </div>
    </section>
  `
})
export class DashboardPage implements OnInit {
  private readonly getAllClassrooms = inject<GetAllClassroomsUseCase>(GET_ALL_CLASSROOMS_USE_CASE);
  private readonly getAvailableClassrooms = inject<GetAvailableClassroomsUseCase>(
    GET_AVAILABLE_CLASSROOMS_USE_CASE
  );
  private readonly getRequests = inject<GetClassroomRequestsUseCase>(GET_CLASSROOM_REQUESTS_USE_CASE);

  protected readonly periodo = ACADEMIC_PERIOD;

  protected readonly totalAulas = signal<number | null>(null);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly libresAhora = signal<number | null>(null);
  protected readonly mensajeDisponibilidad = signal<string | null>(null);

  protected readonly pendientes = signal<number | null>(null);
  protected readonly errorSolicitudes = signal<string | null>(null);

  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected rangoAhora(): string {
    const inicio = new Date();
    const fin = new Date(inicio.getTime() + 2 * 60 * 60 * 1000);
    const hora = (fecha: Date) => fecha.toLocaleTimeString().slice(0, 5);
    return `${hora(inicio)}–${hora(fin)}`;
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCatalogo.set(null);
    this.mensajeDisponibilidad.set(null);
    this.errorSolicitudes.set(null);

    this.getAllClassrooms.execute({}).subscribe({
      next: aulas => this.totalAulas.set(aulas.length),
      error: error =>
        this.errorCatalogo.set(
          `Catálogo no disponible en este momento (HTTP ${error.status ?? '—'}).`
        )
    });

    const inicio = new Date();
    const fin = new Date(inicio.getTime() + 2 * 60 * 60 * 1000);
    this.getAvailableClassrooms.execute(inicio, fin, {}).subscribe({
      next: disponibilidad => this.libresAhora.set(disponibilidad.classrooms.length),
      error: error => {
        if (error.status === 503) {
          this.mensajeDisponibilidad.set(
            'La sincronización con SIGA está desactualizada; el personal administrativo debe lanzarla.'
          );
        } else {
          this.mensajeDisponibilidad.set(`Disponibilidad no calculable ahora (HTTP ${error.status ?? '—'}).`);
        }
      }
    });

    this.getRequests.execute('PENDIENTE').subscribe({
      next: solicitudes => this.pendientes.set(solicitudes.length),
      error: error =>
        this.errorSolicitudes.set(
          `Solicitudes no consultables ahora (HTTP ${error.status ?? '—'}).`
        )
    });
  }
}
