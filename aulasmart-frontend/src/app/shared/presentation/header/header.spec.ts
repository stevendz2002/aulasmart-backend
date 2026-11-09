import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { Header } from './header';
import { AuthService } from '../../../core/auth-service';
import { GET_ALL_CLASSROOMS_USE_CASE } from '../../../features/aulas/application/classrooms-di';
import { Classroom } from '../../../features/aulas/domain/entities/classroom';

const catalogo = { id: 1, externalCode: 1, name: 'Bloque B' };
const aula: Classroom = { id: 12, externalCode: 195, name: 'B101', capacity: 50, building: catalogo, type: catalogo, department: catalogo };
describe('Búsqueda principal', () => {
  const navigate = vi.fn().mockResolvedValue(true);
  const rol = signal('DOCENTE');
  beforeEach(() => {
    navigate.mockClear(); rol.set('DOCENTE');
    TestBed.configureTestingModule({ providers: [
      { provide: Router, useValue: { navigate } },
      { provide: AuthService, useValue: { sesion: signal(null), tieneAlgunRol: (roles: string[]) => roles.includes(rol()) } },
      { provide: GET_ALL_CLASSROOMS_USE_CASE, useValue: { execute: () => of([aula]) } }
    ] });
  });
  it('abre detalles del resultado seleccionado, no del texto parcial', () => {
    const header = TestBed.runInInjectionContext(() => new Header());
    header['consultar']('Bloque B');
    expect(header['resultados']()).toEqual([aula]);
    header['irAAula'](aula, 'detalle');
    expect(navigate).toHaveBeenCalledWith(['/consulta'], { queryParams: { name: 'B101', aula: 12, accion: 'detalle' } });
    expect(header['consulta']()).toBe('');
  });
  it('ofrece solicitud solo a roles autorizados', () => {
    const header = TestBed.runInInjectionContext(() => new Header());
    header['irAAula'](aula, 'solicitar');
    expect(navigate).toHaveBeenCalledWith(['/consulta'], expect.objectContaining({ queryParams: expect.objectContaining({ accion: 'solicitar' }) }));
    navigate.mockClear(); rol.set('ESTUDIANTE');
    header['irAAula'](aula, 'solicitar');
    expect(navigate).not.toHaveBeenCalled();
    expect(header['puedeSolicitar']()).toBe(false);
  });
});
