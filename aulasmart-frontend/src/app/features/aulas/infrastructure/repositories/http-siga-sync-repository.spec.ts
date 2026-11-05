import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { HttpSigaSyncRepository } from './http-siga-sync-repository';
import { SigaSyncRun } from '../../domain/entities/siga-sync-run';

describe('HttpSigaSyncRepository', () => {
  let repository: HttpSigaSyncRepository;
  let requests: HttpTestingController;

  const corridaDto = {
    recurso: 'AULAS',
    estado: 'EXITOSA',
    registrosProcesados: 104,
    iniciadaEn: '2026-10-31T04:33:42Z',
    finalizadaEn: '2026-10-31T04:33:45Z',
    duracionMilisegundos: 3132,
    detalleError: null
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    repository = TestBed.inject(HttpSigaSyncRepository);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('dispara POST con recurso=AULAS y mapea la corrida al dominio', () => {
    let corridas: SigaSyncRun[] | undefined;
    repository.trigger('AULAS').subscribe(result => (corridas = result));

    const req = requests.expectOne(
      http =>
        http.url === 'http://localhost:8080/api/sincronizaciones' &&
        http.params.get('recurso') === 'AULAS'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush([corridaDto]);

    expect(corridas).toEqual([
      {
        resource: 'AULAS',
        status: 'EXITOSA',
        recordsProcessed: 104,
        startedAt: '2026-10-31T04:33:42Z',
        finishedAt: '2026-10-31T04:33:45Z',
        durationMilliseconds: 3132,
        errorDetail: null
      }
    ]);
  });

  it('dispara POST con recurso=EVENTOS para la ocupación académica', () => {
    let corridas: SigaSyncRun[] | undefined;
    repository.trigger('EVENTOS').subscribe(result => (corridas = result));

    const req = requests.expectOne(
      http =>
        http.url === 'http://localhost:8080/api/sincronizaciones' &&
        http.params.get('recurso') === 'EVENTOS'
    );
    req.flush([
      {
        recurso: 'EVENTOS',
        estado: 'EXITOSA',
        registrosProcesados: 812,
        iniciadaEn: '2026-11-03T11:00:00Z',
        finalizadaEn: '2026-11-03T11:00:41Z',
        duracionMilisegundos: 41000,
        detalleError: null
      }
    ]);

    expect(corridas?.[0].resource).toBe('EVENTOS');
    expect(corridas?.[0].recordsProcessed).toBe(812);
  });
});
