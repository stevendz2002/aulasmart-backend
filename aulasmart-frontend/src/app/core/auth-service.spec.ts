import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth-service';

describe('Sesión JWT', () => {
  let auth: AuthService;
  let requests: HttpTestingController;
  const session = { correo: 'docente@example.edu', nombre: 'Docente', rol: 'DOCENTE', accessToken: 'jwt' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    requests = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });
  afterEach(() => requests.verify());
  it('envía credenciales solo al login y conserva access solo en memoria', async () => {
    const result = firstValueFrom(auth.login(session.correo, 'password'));
    const req = requests.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ correo: session.correo, password: 'password' });
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.headers.get('X-AulaSmart-CSRF')).toBe('1');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush(session);
    await result;
    expect(auth.sesion()).toEqual(session);
    expect(sessionStorage.getItem('aulasmart.sesion')).toBeNull();
  });
  it('restaura al recargar usando la cookie sin almacenar contraseñas', async () => {
    const result = auth.restore();
    requests.expectOne('/api/auth/refresh').flush(session);
    await result;
    expect(auth.sesion()?.correo).toBe(session.correo);
  });
  it('comparte una renovación entre peticiones simultáneas', async () => {
    const a = firstValueFrom(auth.refresh());
    const b = firstValueFrom(auth.refresh());
    requests.expectOne('/api/auth/refresh').flush(session);
    expect(await a).toEqual(await b);
  });
  it('revoca en servidor antes de confirmar cierre local', async () => {
    auth.sesion.set(session);
    const result = firstValueFrom(auth.logout());
    expect(auth.sesion()).toEqual(session);
    requests.expectOne('/api/auth/logout').flush(null);
    await result;
    expect(auth.sesion()).toBeNull();
  });
  it('no afirma cierre exitoso si falla la red', async () => {
    auth.sesion.set(session);
    const result = firstValueFrom(auth.logout());
    const rejection = expect(result).rejects.toBeDefined();
    requests.expectOne('/api/auth/logout').flush({}, { status: 503, statusText: 'Unavailable' });
    await rejection;
    expect(auth.sesion()).toEqual(session);
  });
  it('una renovación tardía no restaura una sesión que ya se cerró', async () => {
    const refreshing = firstValueFrom(auth.refresh());
    auth.clear();
    requests.expectOne('/api/auth/refresh').flush(session);
    await refreshing;
    expect(auth.sesion()).toBeNull();
  });
  it('una cookie vencida deja disponible el inicio de sesión', async () => {
    const result = auth.restore();
    requests.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    await result;
    expect(auth.sesion()).toBeNull();
  });
});
