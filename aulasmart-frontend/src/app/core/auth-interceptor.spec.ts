import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth-service';
import { authInterceptor } from './auth-interceptor';

describe('Credenciales limitadas a AulaSmart', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let auth: AuthService;
  const session = { correo: 'docente', nombre: null, rol: 'DOCENTE', accessToken: 'anterior' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()
    ] });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth.sesion.set(session);
  });
  afterEach(() => requests.verify());
  it('adjunta Bearer únicamente a la API y excluye autenticación', () => {
    for (const url of ['/api/aulas', 'https://otro.example/aulas', '/api-externa/aulas', '/api/auth/refresh']) {
      http.get(url).subscribe();
      const req = requests.expectOne(url);
      expect(req.request.headers.get('Authorization')).toBe(url === '/api/aulas' ? 'Bearer anterior' : null);
      req.flush([]);
    }
  });
  it('no reemplaza una cabecera explícita', () => {
    http.get('/api/usuarios/whoami', { headers: { Authorization: 'Bearer otra' } }).subscribe();
    const req = requests.expectOne('/api/usuarios/whoami');
    expect(req.request.headers.get('Authorization')).toBe('Bearer otra');
    req.flush({});
  });
  it('renueva una vez y reintenta las peticiones concurrentes', async () => {
    const a = firstValueFrom(http.get('/api/aulas'));
    const b = firstValueFrom(http.get('/api/usuarios/whoami'));
    requests.expectOne('/api/aulas').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/usuarios/whoami').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'nuevo' });
    await vi.waitFor(() => expect(auth.sesion()?.accessToken).toBe('nuevo'));
    for (const url of ['/api/aulas', '/api/usuarios/whoami']) {
      const retry = requests.expectOne(url);
      expect(retry.request.headers.get('Authorization')).toBe('Bearer nuevo');
      retry.flush({});
    }
    await Promise.all([a,b]);
  });
  it('una renovación rechazada cierra la sesión sin bucle', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const result = firstValueFrom(http.get('/api/aulas'));
    const rejected = expect(result).rejects.toBeDefined();
    requests.expectOne('/api/aulas').flush({}, { status: 401, statusText: 'Unauthorized' });
    requests.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    await rejected;
    expect(auth.sesion()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
  it('no renueva ni cierra sesión ante un permiso denegado', async () => {
    const result = firstValueFrom(http.get('/api/usuarios'));
    const rejected = expect(result).rejects.toBeDefined();
    requests.expectOne('/api/usuarios').flush({}, { status: 403, statusText: 'Forbidden' });
    await rejected;
    requests.expectNone('/api/auth/refresh');
    expect(auth.sesion()).toEqual(session);
  });

  it('no reintenta una petición anterior con la identidad de otra sesión', async () => {
    const result = firstValueFrom(http.get('/api/aulas'));
    const rejected = expect(result).rejects.toBeDefined();
    requests.expectOne('/api/aulas').flush({}, { status: 401, statusText: 'Unauthorized' });
    auth.clear();
    auth.sesion.set({ ...session, correo: 'otra', accessToken: 'otra-sesion' });
    requests.expectOne('/api/auth/refresh').flush({ ...session, accessToken: 'tardio' });
    await rejected;
    requests.expectNone('/api/aulas');
    expect(auth.sesion()?.correo).toBe('otra');
  });
});
