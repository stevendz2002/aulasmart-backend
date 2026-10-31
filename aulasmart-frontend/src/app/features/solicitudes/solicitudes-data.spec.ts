import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { SolicitudesData } from './solicitudes-data';

describe('SolicitudesData', () => {
  let servicio: SolicitudesData;
  let peticiones: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    servicio = TestBed.inject(SolicitudesData);
    peticiones = TestBed.inject(HttpTestingController);
  });

  afterEach(() => peticiones.verify());

  it('lista todas las solicitudes sin filtro', () => {
    servicio.listar().subscribe();

    peticiones.expectOne('http://localhost:8080/api/solicitudes').flush([]);
  });

  it('omite el filtro cuando el estado es TODAS', () => {
    servicio.listar('TODAS').subscribe();

    peticiones.expectOne('http://localhost:8080/api/solicitudes').flush([]);
  });

  it('aplica el filtro de estado', () => {
    servicio.listar('PENDIENTE').subscribe();

    peticiones.expectOne(
      http => http.url === 'http://localhost:8080/api/solicitudes' && http.params.get('estado') === 'PENDIENTE'
    ).flush([]);
  });

  it('crea la reserva con el cuerpo del contrato [aulaId, inicio, fin, motivo]', () => {
    let respuesta = null;
    servicio
      .crear({
        aulaId: 9004,
        inicio: new Date('2026-11-11T14:00:00Z'),
        fin: new Date('2026-11-11T16:00:00Z'),
        motivo: 'Monitoria de cálculo'
      })
      .subscribe(r => (respuesta = r));

    const req = peticiones.expectOne('http://localhost:8080/api/solicitudes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      aulaId: 9004,
      inicio: '2026-11-11T14:00:00.000Z',
      fin: '2026-11-11T16:00:00.000Z',
      motivo: 'Monitoria de cálculo'
    });
    req.flush({ id: 9003, estado: 'PENDIENTE' });

    expect(respuesta).toEqual({ id: 9003, estado: 'PENDIENTE' });
  });

  it('envía la decisión de revisión al endpoint específico', () => {
    servicio.revisar(9002, 'APROBAR').subscribe();

    const req = peticiones.expectOne('http://localhost:8080/api/solicitudes/9002/revision');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'APROBAR' });
    req.flush({ id: 9002, estado: 'APROBADA' });
  });
});
