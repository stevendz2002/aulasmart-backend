import { signal } from '@angular/core';
import { AuthService } from '../../../../core/auth-service';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { ConsultaPage } from './consulta-page';
import { GET_ALL_CLASSROOMS_USE_CASE, GET_AVAILABLE_CLASSROOMS_USE_CASE, GET_CLASSROOM_OCCUPATION_USE_CASE } from '../../application/classrooms-di';
import { CREATE_CLASSROOM_REQUEST_USE_CASE } from '../../../solicitudes/application/classroom-requests-di';
import { Classroom } from '../../domain/entities/classroom';
import { ClassroomOccupation } from '../../domain/entities/occupation';
import { ClassroomCardBuilder } from '../models/classroom-card';

describe('Consulta: disponibilidad verificable', () => {
  const catalogo = { id: 1, externalCode: 1, name: 'Catálogo' };
  const aula: Classroom = { id: 1, externalCode: 1, name: 'Aula', capacity: 30, building: catalogo, type: catalogo, department: catalogo };
  let page: ConsultaPage;
  const execute = vi.fn();
  const disponibilidad = vi.fn();
  const crear = vi.fn();
  const rol = signal('DOCENTE');
  const parametros = new BehaviorSubject(convertToParamMap({}));
  const navegar = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    parametros.next(convertToParamMap({}));
    navegar.mockClear();
    execute.mockReset();
    crear.mockReset();
    rol.set('DOCENTE');
    disponibilidad.mockReset();
    TestBed.configureTestingModule({ providers: [
      { provide: ActivatedRoute, useValue: { queryParamMap: parametros } },
      { provide: Router, useValue: { navigate: navegar } },
      { provide: AuthService, useValue: { tieneAlgunRol: (roles: string[]) => roles.includes(rol()) } },
      { provide: GET_ALL_CLASSROOMS_USE_CASE, useValue: { execute: () => of([aula]) } },
      { provide: GET_AVAILABLE_CLASSROOMS_USE_CASE, useValue: { execute: disponibilidad } },
      { provide: GET_CLASSROOM_OCCUPATION_USE_CASE, useValue: { execute } },
      { provide: CREATE_CLASSROOM_REQUEST_USE_CASE, useValue: { execute: crear } }
    ] });
    page = TestBed.runInInjectionContext(() => new ConsultaPage());
    page['catalogo'].set([aula]);
    page['fechaConsulta'].set('2026-11-10');
  });

  it('no muestra franjas libres cuando falla la consulta', () => {
    execute.mockReturnValue(throwError(() => ({ status: 503 })));
    page['abrirDetalle'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    expect(page['timeline']()).toEqual([]);
    expect(page['franjasLibres']()).toEqual([]);
    expect(page['errorOcupacion']()).toContain('No se puede verificar');
  });

  it('consulta la fecha seleccionada en Colombia y distingue vacío válido de error', () => {
    execute.mockReturnValue(of([]));
    page['abrirDetalle'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    expect(execute).toHaveBeenCalledWith(1, {
      start: new Date('2026-11-10T05:00:00Z'), end: new Date('2026-11-11T05:00:00Z')
    });
    expect(page['errorOcupacion']()).toBeNull();
    expect(page['franjasLibres']().length).toBeGreaterThan(0);
  });

  it('ignora respuestas anteriores al cambiar la fecha de la solicitud', () => {
    const anterior = new Subject<ClassroomOccupation[]>();
    execute.mockReturnValueOnce(anterior).mockReturnValueOnce(throwError(() => ({ status: 503 })));
    page['abrirSolicitudDirecta'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    page['cambiarFechaSolicitud']('2026-11-12');
    anterior.next([]);
    expect(page['franjasLibres']()).toEqual([]);
    expect(page['errorOcupacion']()).not.toBeNull();
  });

  it('descarta disponibilidad anterior cuando falla una nueva búsqueda', () => {
    page['disponibilidad'].set({ range: { start: '', end: '', verifiedAt: '' }, classrooms: [aula] });
    disponibilidad.mockReturnValue(throwError(() => ({ status: 503 })));
    page['consultar']();
    expect(page['disponibilidad']()).toBeNull();
    expect(page['hayaDisponibilidad']()).toBe(false);
  });
  it('el estudiante consulta detalles pero no puede abrir ni enviar una solicitud', () => {
    rol.set('ESTUDIANTE'); execute.mockReturnValue(of([]));
    page['abrirDetalle'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    expect(page['detalleConstruido']()?.name).toBe('Aula');
    page['abrirSolicitud']();
    page['abrirSolicitudDirecta'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    page['enviarSolicitud'](new Event('submit'));
    expect(page['modalSolicitudAbierto']()).toBe(false);
    expect(crear).not.toHaveBeenCalled();
  });

  it('conserva el identificador y estado y evita envíos repetidos', () => {
    execute.mockReturnValue(of([]));
    const respuesta = new Subject<any>(); crear.mockReturnValue(respuesta);
    page['abrirSolicitudDirecta'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    page['enviarSolicitud'](new Event('submit'));
    page['enviarSolicitud'](new Event('submit'));
    page['cerrarSolicitud']();
    expect(page['modalSolicitudAbierto']()).toBe(true);
    expect(crear).toHaveBeenCalledTimes(1);
    expect(crear.mock.calls[0][0].start).toEqual(new Date('2026-11-10T13:00:00Z'));
    respuesta.next({ id: 77, status: 'PENDIENTE' });
    page['enviarSolicitud'](new Event('submit'));
    expect(crear).toHaveBeenCalledTimes(1);
    expect(page['solicitudCreadaId']()).toBe(77);
    expect(page['exitoSolicitud']()).toContain('PENDIENTE');
    expect(page['modalSolicitudAbierto']()).toBe(true);
  });

  it.each([409, 503])('permite corregir o reintentar ante error %s sin confirmar', status => {
    execute.mockReturnValue(of([])); crear.mockReturnValue(throwError(() => ({ status })));
    page['abrirSolicitudDirecta'](ClassroomCardBuilder.of(aula, 'LIBRE'));
    page['enviarSolicitud'](new Event('submit'));
    expect(page['errorSolicitud']()).toBeTruthy();
    expect(page['exitoSolicitud']()).toBeNull();
    expect(page['enviandoSolicitud']()).toBe(false);
    expect(page['solicitudCreadaId']()).toBeNull();
  });
  it('actualiza la búsqueda estando ya en consulta y abre el aula por ID', () => {
    execute.mockReturnValue(of([]));
    disponibilidad.mockReturnValue(of({ range: { start: '', end: '', verifiedAt: '' }, classrooms: [] }));
    page.ngOnInit();
    parametros.next(convertToParamMap({ name: 'Aula', aula: '1', accion: 'detalle' }));
    expect(page['detalle']()?.id).toBe(1);
    parametros.next(convertToParamMap({ name: 'Nueva búsqueda' }));
    expect(page['textoBusqueda']()).toBe('Nueva búsqueda');
    expect(page['detalle']()).toBeNull();
    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ replaceUrl: true }));
  });

  it('espera al catálogo al entrar desde la búsqueda principal y abre solicitud', () => {
    execute.mockReturnValue(of([]));
    disponibilidad.mockReturnValue(of({ range: { start: '', end: '', verifiedAt: '' }, classrooms: [] }));
    page['catalogo'].set(null);
    parametros.next(convertToParamMap({ name: 'Aula', aula: '1', accion: 'solicitar' }));
    page.ngOnInit();
    expect(page['modalSolicitudAbierto']()).toBe(true);
    expect(page['solicitudAula']()?.id).toBe(1);
  });

  it('una URL de solicitar no abre formulario a estudiantes', () => {
    rol.set('ESTUDIANTE'); execute.mockReturnValue(of([]));
    disponibilidad.mockReturnValue(of({ range: { start: '', end: '', verifiedAt: '' }, classrooms: [] }));
    parametros.next(convertToParamMap({ aula: '1', accion: 'solicitar' }));
    page.ngOnInit();
    expect(page['modalSolicitudAbierto']()).toBe(false);
    expect(page['detalle']()?.id).toBe(1);
  });
});
