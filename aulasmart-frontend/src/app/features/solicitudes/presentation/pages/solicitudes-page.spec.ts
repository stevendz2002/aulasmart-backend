import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../../../core/auth-service';
import { SolicitudesPage } from './solicitudes-page';
import { GET_ALL_CLASSROOMS_USE_CASE } from '../../../aulas/application/classrooms-di';
import { CREATE_CLASSROOM_REQUEST_USE_CASE, GET_CLASSROOM_REQUESTS_USE_CASE, REVIEW_CLASSROOM_REQUEST_USE_CASE } from '../../application/classroom-requests-di';
import { ClassroomRequest } from '../../domain/entities/classroom-request';

describe('Seguimiento de solicitudes', () => {
  const rol = signal('ADMINISTRADOR');
  const revisar = vi.fn(); const listar = vi.fn();
  const solicitud: ClassroomRequest = { id: 77, classroomId: 1, requesterId: 5, start: '2026-11-10T13:00:00Z', end: '2026-11-10T15:00:00Z', status: 'PENDIENTE', reason: 'Clase', reviewedById: null, reviewedAt: null, createdAt: '' };
  beforeEach(() => {
    rol.set('ADMINISTRADOR'); revisar.mockReset(); listar.mockReset().mockReturnValue(of([solicitud]));
    TestBed.configureTestingModule({ imports: [SolicitudesPage], providers: [
      { provide: AuthService, useValue: { tieneAlgunRol: (roles: string[]) => roles.includes(rol()) } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ solicitud: '77' }) } } },
      { provide: GET_ALL_CLASSROOMS_USE_CASE, useValue: { execute: () => of([]) } },
      { provide: CREATE_CLASSROOM_REQUEST_USE_CASE, useValue: {} },
      { provide: GET_CLASSROOM_REQUESTS_USE_CASE, useValue: { execute: listar } },
      { provide: REVIEW_CLASSROOM_REQUEST_USE_CASE, useValue: { execute: revisar } }
    ] });
  });
  it('abre el seguimiento solicitado desde la confirmación', () => {
    const fixture = TestBed.createComponent(SolicitudesPage); fixture.detectChanges();
    expect(fixture.componentInstance['textoBusqueda']()).toBe('SOL-77');
    expect(fixture.nativeElement.textContent).toContain('SOL-77');
    expect(fixture.nativeElement.textContent).toContain('PENDIENTE');
  });
  it('bloquea revisiones repetidas y permite reintentar tras un error', () => {
    const respuesta = new Subject<ClassroomRequest>(); revisar.mockReturnValue(respuesta);
    const page = TestBed.runInInjectionContext(() => new SolicitudesPage());
    page['decidir'](solicitud, 'APROBAR'); page['decidir'](solicitud, 'RECHAZAR');
    expect(revisar).toHaveBeenCalledTimes(1);
    respuesta.error({ status: 503 });
    expect(page['revisando']().size).toBe(0);
    expect(page['mensajeRevision']()).toContain('verificar');
    revisar.mockReturnValue(of({ ...solicitud, status: 'APROBADA' }));
    page['decidir'](solicitud, 'APROBAR');
    expect(page['mensajeRevision']()).toContain('APROBADA');
  });
  it('muestra error y reintento sin indicar carga indefinida', () => {
    listar.mockReturnValue(throwError(() => ({ status: 503 })));
    const fixture = TestBed.createComponent(SolicitudesPage); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
    expect(fixture.nativeElement.textContent).not.toContain('Consultando solicitudes');
  });
  it('un docente no puede invocar la revisión', () => {
    rol.set('DOCENTE');
    const page = TestBed.runInInjectionContext(() => new SolicitudesPage());
    page['decidir'](solicitud, 'APROBAR');
    expect(revisar).not.toHaveBeenCalled();
  });
  it('el enlace SOL-77 selecciona solo ese ID, no coincidencias parciales', () => {
    listar.mockReturnValue(of([solicitud, { ...solicitud, id: 177 }, { ...solicitud, id: 3, reason: 'Grupo 77' }]));
    const fixture = TestBed.createComponent(SolicitudesPage); fixture.detectChanges();
    expect(fixture.componentInstance['visibles']().map(s => s.id)).toEqual([77]);
    fixture.componentInstance['buscar']('Grupo');
    expect(fixture.componentInstance['visibles']().map(s => s.id)).toEqual([3]);
  });
  it.each(['respuesta', 'error'])('ignora %s atrasado tras una recarga más reciente', caso => {
    const anterior = new Subject<ClassroomRequest[]>();
    listar.mockReturnValueOnce(anterior).mockReturnValueOnce(of([{ ...solicitud, status: 'APROBADA' }]));
    const page = TestBed.runInInjectionContext(() => new SolicitudesPage());
    page['cargar']('ALL'); page['cargar']('ALL');
    if (caso === 'respuesta') anterior.next([solicitud]);
    else anterior.error({ status: 503 });
    expect(page['solicitudes']()?.[0].status).toBe('APROBADA');
    expect(page['errorCarga']()).toBeNull();
    expect(page['cargando']()).toBe(false);
  });
  it('conserva la decisión confirmada aunque falle la recarga', () => {
    const page = TestBed.runInInjectionContext(() => new SolicitudesPage());
    page['solicitudes'].set([solicitud]);
    revisar.mockReturnValue(of({ ...solicitud, status: 'APROBADA' }));
    listar.mockReturnValue(throwError(() => ({ status: 503 })));
    page['decidir'](solicitud, 'APROBAR');
    expect(page['solicitudes']()?.[0].status).toBe('APROBADA');
    expect(page['mensajeRevision']()).toContain('APROBADA');
    expect(page['errorCarga']()).toBeTruthy();
  });
});
