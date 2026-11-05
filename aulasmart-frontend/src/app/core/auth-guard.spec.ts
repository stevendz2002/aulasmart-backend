import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { ActivatedRouteSnapshot } from '@angular/router';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth-service';
import { requiereRoles } from './auth-guard';

describe('requiereRoles', () => {
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ providers: [provideRouter([])] }).compileComponents();
    authService = TestBed.inject(AuthService);
    sessionStorage.clear();
  });

  afterEach(() => sessionStorage.clear());

  const ruta = { url: [] } as unknown as ActivatedRouteSnapshot;

  const evaluar = (guarda: Parameters<typeof requiereRoles>[0] extends string[] ? never : ReturnType<typeof requiereRoles>) =>
    TestBed.runInInjectionContext(() => guarda(ruta, { url: [] } as never)) as boolean | UrlTree;

  it('sin sesion redirige a /login', () => {
    const resultado: UrlTree = evaluar(requiereRoles()) as UrlTree;
    expect(resultado.toString()).toBe('/login');
  });

  it('con sesion pero sin el rol pedido redirige al dashboard', () => {
    authService.sesion.set({
      correo: 'docente@prueba.aulasmart.local',
      nombre: null,
      roles: ['DOCENTE'],
      claveCodificada: 'x'
    });
    const resultado = evaluar(requiereRoles('ADMINISTRADOR'));
    expect(resultado instanceof UrlTree ? resultado.toString() : resultado).toBe('/');
  });

  it('con sesion y alguno de los roles requeridos permite el acceso', () => {
    authService.sesion.set({
      correo: 'docente@prueba.aulasmart.local',
      nombre: 'PRUEBA Docente',
      roles: ['DOCENTE'],
      claveCodificada: 'x'
    });
    const resultado = evaluar(requiereRoles('DOCENTE', 'ADMINISTRADOR'));
    expect(resultado).toBe(true);
  });

  it('sin roles requeridos cualquier sesion activa pasa', () => {
    authService.sesion.set({
      correo: 'estudiante@prueba.aulasmart.local',
      nombre: null,
      roles: ['ESTUDIANTE'],
      claveCodificada: 'x'
    });
    const resultado = evaluar(requiereRoles());
    expect(resultado).toBe(true);
  });

  it('el router esta disponible para las redirecciones', () => {
    expect(TestBed.inject(Router)).toBeTruthy();
  });
});
