import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { HttpClassroomRepository } from './http-classroom-repository';
import { Classroom, Availability, ClassroomFilter } from '../../domain/entities/classroom';

describe('HttpClassroomRepository', () => {
  let repository: HttpClassroomRepository;
  let requests: HttpTestingController;

  const classroomDto = {
    id: 9004,
    codigoSiga: 406,
    nombre: 'AULAS SEDE CALI (406)',
    edificio: { id: 9003, codigoSiga: 48, nombre: 'MULTICAMPUS LADERA' },
    capacidad: 200,
    tipoAula: { id: 9003, codigoSiga: 1, nombre: 'SALON DE CLASE' },
    dependencia: { id: 9002, codigoSiga: 14, nombre: 'INFORMATICA' }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    repository = TestBed.inject(HttpClassroomRepository);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('lista el catálogo sin filtros y mapea el DTO al dominio', () => {
    let aulas: Classroom[] | undefined;
    repository.getAll({}).subscribe(result => (aulas = result));

    requests.expectOne('http://localhost:8080/api/aulas').flush([classroomDto]);

    expect(aulas).toEqual([
      {
        id: 9004,
        externalCode: 406,
        name: 'AULAS SEDE CALI (406)',
        building: { id: 9003, externalCode: 48, name: 'MULTICAMPUS LADERA' },
        capacity: 200,
        type: { id: 9003, externalCode: 1, name: 'SALON DE CLASE' },
        department: { id: 9002, externalCode: 14, name: 'INFORMATICA' }
      }
    ]);
  });

  it('traduce el filtro del dominio al query param real capacidadMinima', () => {
    const filter: ClassroomFilter = { minCapacity: 50, buildingId: 9 };
    repository.getAll(filter).subscribe();

    const req = requests.expectOne(
      http =>
        http.url === 'http://localhost:8080/api/aulas' &&
        http.params.get('capacidadMinima') === '50' &&
        http.params.get('idEdificio') === '9'
    );
    req.flush([]);
  });

  it('consulta disponibilidad traduciendo inicio/fin al contrato real', () => {
    let disponibilidad: Availability | undefined;
    repository
      .getAvailable(
        { start: new Date('2026-11-10T14:00:00Z'), end: new Date('2026-11-10T16:00:00Z') },
        { minCapacity: 100 }
      )
      .subscribe(result => (disponibilidad = result));

    const req = requests.expectOne(
      http =>
        http.url === 'http://localhost:8080/api/aulas/disponibles' &&
        http.params.get('inicio') === '2026-11-10T14:00:00.000Z' &&
        http.params.get('fin') === '2026-11-10T16:00:00.000Z' &&
        http.params.get('capacidadMinima') === '100'
    );
    req.flush({
      inicio: '2026-11-10T14:00:00Z',
      fin: '2026-11-10T16:00:00Z',
      verificadoEn: '2026-10-31T04:34:16Z',
      aulas: [classroomDto]
    });

    expect((disponibilidad as Availability).range).toEqual({
      start: '2026-11-10T14:00:00Z',
      end: '2026-11-10T16:00:00Z',
      verifiedAt: '2026-10-31T04:34:16Z'
    });
    expect((disponibilidad as Availability).classrooms[0].id).toBe(9004);
  });
});
