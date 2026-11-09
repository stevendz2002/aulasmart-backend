import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
import { DashboardPage } from './dashboard-page';
import { AuthService } from '../../../../core/auth-service';
import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE } from '../../../aulas/application/classrooms-di';
import { GET_CLASSROOM_REQUESTS_USE_CASE } from '../../../solicitudes/application/classroom-requests-di';

describe('Recuperación de disponibilidad del panel', () => {
  const disponibilidad = vi.fn();
  beforeEach(() => {
    vi.useFakeTimers(); disponibilidad.mockReset();
    TestBed.configureTestingModule({ providers: [
      { provide: AuthService, useValue: { sesion: signal(null) } },
      { provide: GET_ALL_CLASSROOMS_USE_CASE, useValue: { execute: () => of([]) } },
      { provide: GET_AVAILABLE_CLASSROOMS_USE_CASE, useValue: { execute: disponibilidad } },
      { provide: GET_CLASSROOM_REQUESTS_USE_CASE, useValue: { execute: () => of([]) } }
    ] });
  });
  afterEach(() => { TestBed.resetTestingModule(); vi.useRealTimers(); });
  it('se recupera de 503 sin recargar la página ni declarar desactualización', () => {
    disponibilidad.mockReturnValueOnce(throwError(() => ({ status: 503 })))
      .mockReturnValue(of({ classrooms: [], range: {} }));
    const page = TestBed.runInInjectionContext(() => new DashboardPage()); page.ngOnInit();
    expect(page['libres']()).toBeNull();
    expect(page['mensajeDisponibilidad']()).toContain('sincronizándose');
    vi.advanceTimersByTime(30000);
    expect(page['libres']()).toBe(0);
    expect(page['mensajeDisponibilidad']()).toBeNull();
    TestBed.resetTestingModule();
    vi.advanceTimersByTime(60000);
    expect(disponibilidad).toHaveBeenCalledTimes(2);
  });
  it('no solapa peticiones y elimina valores antiguos si falla la siguiente', () => {
    const pendiente = new Subject<any>(); disponibilidad.mockReturnValue(pendiente);
    const page = TestBed.runInInjectionContext(() => new DashboardPage()); page.ngOnInit();
    vi.advanceTimersByTime(60000);
    expect(disponibilidad).toHaveBeenCalledTimes(1);
    pendiente.next({ classrooms: [{}] }); pendiente.complete();
    expect(page['libres']()).toBe(1);
    disponibilidad.mockReturnValue(throwError(() => ({ status: 503 })));
    vi.advanceTimersByTime(30000);
    expect(page['libres']()).toBeNull();
    expect(page['libresCampus']()).toEqual([]);
  });
});
