import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth-service';
import { authInterceptor } from './auth-interceptor';
import { apiUrl } from './api-url';

describe('Credenciales limitadas a AulaSmart', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()
    ] });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).sesion.set({ correo: 'docente', nombre: null, rol: 'DOCENTE', claveCodificada: 'credencial' });
  });
  afterEach(() => requests.verify());

  it('adjunta Basic a la API pero no a destinos externos o rutas con prefijo engañoso', () => {
    for (const url of [`${apiUrl}/aulas`, 'https://otro.example/aulas', `${apiUrl}-externa/aulas`]) {
      http.get(url).subscribe();
      const request = requests.expectOne(url);
      expect(request.request.headers.get('Authorization')).toBe(url === `${apiUrl}/aulas` ? 'Basic credencial' : null);
      request.flush([]);
    }
  });

  it('no reemplaza las credenciales de un nuevo inicio de sesión', () => {
    http.get(`${apiUrl}/usuarios/whoami`, { headers: { Authorization: 'Basic nueva' } }).subscribe();
    const request = requests.expectOne(`${apiUrl}/usuarios/whoami`);
    expect(request.request.headers.get('Authorization')).toBe('Basic nueva');
    request.flush({});
  });
});
