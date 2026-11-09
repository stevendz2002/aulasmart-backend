import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
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

  beforeEach(() => {
    execute.mockReset();
    disponibilidad.mockReset();
    TestBed.configureTestingModule({ providers: [
      { provide: ActivatedRoute, useValue: {} },
      { provide: GET_ALL_CLASSROOMS_USE_CASE, useValue: {} },
      { provide: GET_AVAILABLE_CLASSROOMS_USE_CASE, useValue: { execute: disponibilidad } },
      { provide: GET_CLASSROOM_OCCUPATION_USE_CASE, useValue: { execute } },
      { provide: CREATE_CLASSROOM_REQUEST_USE_CASE, useValue: {} }
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
});
