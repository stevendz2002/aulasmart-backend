import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { HttpClassroomRequestRepository } from './http-classroom-request-repository';
import { ClassroomRequest } from '../../domain/entities/classroom-request';

describe('HttpClassroomRequestRepository', () => {
  let repository: HttpClassroomRequestRepository;
  let requests: HttpTestingController;

  const requestDto = {
    id: 9001,
    aulaId: 9002,
    docenteId: 9001,
    inicio: '2026-11-10T15:00:00Z',
    fin: '2026-11-10T17:00:00Z',
    estado: 'APROBADA' as const,
    motivo: 'PRUEBA: actividad académica aprobada.',
    revisadaPorId: 9002,
    revisadaEn: '2026-10-29T14:00:00Z',
    creadaEn: '2026-10-29T17:42:01.994640Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    repository = TestBed.inject(HttpClassroomRequestRepository);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('lista todas las solicitudes sin filtro y mapea al dominio', () => {
    let solicitudes: ClassroomRequest[] | undefined;
    repository.list().subscribe(result => (solicitudes = result));

    requests.expectOne('http://localhost:8080/api/solicitudes').flush([requestDto]);

    expect(solicitudes).toEqual([
      {
        id: 9001,
        classroomId: 9002,
        requesterId: 9001,
        start: '2026-11-10T15:00:00Z',
        end: '2026-11-10T17:00:00Z',
        status: 'APROBADA',
        reason: 'PRUEBA: actividad académica aprobada.',
        reviewedById: 9002,
        reviewedAt: '2026-10-29T14:00:00Z',
        createdAt: '2026-10-29T17:42:01.994640Z'
      }
    ]);
  });

  it('omite el parámetro cuando el filtro es ALL y lo envía con estado concreto', () => {
    repository.list('ALL').subscribe();
    requests.expectOne('http://localhost:8080/api/solicitudes').flush([]);

    repository.list('PENDIENTE').subscribe();
    requests.expectOne(
      http => http.params.get('estado') === 'PENDIENTE' && http.url === 'http://localhost:8080/api/solicitudes'
    ).flush([]);
  });

  it('crea la solicitud con el cuerpo real del contrato', () => {
    let creada: ClassroomRequest | undefined;
    repository
      .create({
        classroomId: 9004,
        start: new Date('2026-11-11T14:00:00Z'),
        end: new Date('2026-11-11T16:00:00Z'),
        reason: 'Monitoria de cálculo'
      })
      .subscribe(result => (creada = result));

    const req = requests.expectOne('http://localhost:8080/api/solicitudes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      aulaId: 9004,
      inicio: '2026-11-11T14:00:00.000Z',
      fin: '2026-11-11T16:00:00.000Z',
      motivo: 'Monitoria de cálculo'
    });
    req.flush({ ...requestDto, id: 9003, estado: 'PENDIENTE', motivo: 'Monitoria de cálculo' });

    expect(creada?.id).toBe(9003);
    expect(creada?.status).toBe('PENDIENTE');
  });

  it('envía la decisión al endpoint de revisión con el cuerpo real', () => {
    repository.review(9002, 'APROBAR').subscribe();

    const req = requests.expectOne('http://localhost:8080/api/solicitudes/9002/revision');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'APROBAR' });
    req.flush(requestDto);
  });
});
