import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AulasData } from './aulas-data';
import { Aula } from '../../core/modelos';

describe('AulasData', () => {
  let servicio: AulasData;
  let peticiones: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    servicio = TestBed.inject(AulasData);
    peticiones = TestBed.inject(HttpTestingController);
  });

  afterEach(() => peticiones.verify());

  it('lista el catálogo sin filtros', () => {
    let aulas: Aula[] | undefined;
    servicio.listar().subscribe(r => (aulas = r));

    peticiones.expectOne('http://localhost:8080/api/aulas').flush([]);

    expect(aulas).toEqual([]);
  });

  it('lista aplicando capacidad mínima como query param', () => {
    servicio.listar({ capacidadMinima: 50 }).subscribe();

    const req = peticiones.expectOne(
      http => http.url === 'http://localhost:8080/api/aulas' && http.params.get('capacidadMinima') === '50'
    );
    req.flush([]);
  });

  it('consulta disponibilidad con el intervalo en ISO-8601', () => {
    let disponibilidad = null;
    servicio
      .disponibles(new Date('2026-11-10T14:00:00Z'), new Date('2026-11-10T16:00:00Z'), { capacidadMinima: 100 })
      .subscribe(r => (disponibilidad = r));

    const req = peticiones.expectOne(
      http =>
        http.url === 'http://localhost:8080/api/aulas/disponibles' &&
        http.params.get('inicio') === '2026-11-10T14:00:00.000Z' &&
        http.params.get('fin') === '2026-11-10T16:00:00.000Z' &&
        http.params.get('capacidadMinima') === '100'
    );
    req.flush({ inicio: 'a', fin: 'b', verificadoEn: 'c', aulas: [] });

    expect(disponibilidad).toEqual({ inicio: 'a', fin: 'b', verificadoEn: 'c', aulas: [] });
  });
});
