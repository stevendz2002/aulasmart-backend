import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AuthService } from './auth-service';

describe('AuthService', () => {
  let service: AuthService;
  let peticiones: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    service = TestBed.inject(AuthService);
    peticiones = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });

  afterEach(() => {
    peticiones.verify();
    sessionStorage.clear();
  });

  it('login adjunta la credencial Basic y publica la sesion', () => {
    let sesionRecibida: { correo: string; roles: string[]; claveCodificada: string } | null = null;
    service.login('docente@prueba.aulasmart.local', 'password').subscribe(s => (sesionRecibida = s));

    const req = peticiones.expectOne('http://localhost:8080/api/usuarios/whoami');
    expect(req.request.headers.get('Authorization')).toBe(
      'Basic ' + btoa('docente@prueba.aulasmart.local:password')
    );
    req.flush({ correo: 'docente@prueba.aulasmart.local', roles: ['DOCENTE'] });

    expect(sesionRecibida!.correo).toBe('docente@prueba.aulasmart.local');
    expect(service.sesion()?.roles).toEqual(['DOCENTE']);
    expect(sessionStorage.getItem('aulasmart.sesion')).toContain('docente@prueba.aulasmart.local');
  });

  it('logout limpia la sesion y el almacen local', () => {
    service.login('docente@prueba.aulasmart.local', 'password').subscribe();
    peticiones.expectOne('http://localhost:8080/api/usuarios/whoami')
      .flush({ correo: 'docente@prueba.aulasmart.local', roles: ['DOCENTE'] });

    service.logout();

    expect(service.sesion()).toBeNull();
    expect(sessionStorage.getItem('aulasmart.sesion')).toBeNull();
  });
});
