import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { AuthService } from '../../../core/auth-service';
import { ShellState } from '../shell-state';
import { SOLICITUDES_PROVIDERS } from '../../../features/solicitudes/infrastructure/solicitudes-providers';
import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let authService: AuthService;
  let requests: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), ...SOLICITUDES_PROVIDERS]
    }).compileComponents();
    authService = TestBed.inject(AuthService);
    requests = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });

  afterEach(() => {
    requests.match(() => true).forEach(req => req.flush([]));
    requests.verify();
    sessionStorage.clear();
  });

  function fixture() {
    return TestBed.createComponent(Sidebar);
  }

  it('muestra el grupo General para cualquier sesión (incluso sin sesión)', () => {
    authService.sesion.set(null);
    const f = fixture();
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.titulo-grupo')).map(e => e.nativeElement.textContent);
    expect(textos).toContain('General');
    expect(textos).not.toContain('Administración');
  });

  it('no muestra Administración a un DOCENTE', () => {
    authService.sesion.set({
      correo: 'docente@prueba.aulasmart.local',
      nombre: 'PRUEBA Docente',
      roles: ['DOCENTE'],
      claveCodificada: 'x'
    });
    const f = fixture();
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.titulo-grupo')).map(e => e.nativeElement.textContent);
    expect(textos).not.toContain('Administración');
  });

  it('ADMINISTRATIVO ve Administración con gestión de aulas pero SIN usuarios', () => {
    authService.sesion.set({
      correo: 'secretaria@prueba.aulasmart.local',
      nombre: 'PRUEBA Revisor',
      roles: ['ADMINISTRATIVO'],
      claveCodificada: 'x'
    });
    const f = fixture();
    f.detectChanges();

    const peticion = requests.expectOne('http://localhost:8080/api/solicitudes?estado=PENDIENTE');
    peticion.flush([]);
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.titulo-grupo')).map(e => e.nativeElement.textContent);
    expect(textos).toContain('Administración');
    const enlaces = f.debugElement
      .queryAll(By.css('a'))
      .map(e => e.nativeElement.getAttribute('href'));
    expect(enlaces).toContain('/aulas');
    expect(enlaces).not.toContain('/usuarios');
  });

  it('ADMINISTRADOR ve Administración con gestión de aulas y usuarios, y badge de pendientes', () => {
    authService.sesion.set({
      correo: 'admin@prueba.aulasmart.local',
      nombre: 'PRUEBA Administrador',
      roles: ['ADMINISTRADOR'],
      claveCodificada: 'x'
    });
    const f = fixture();
    f.detectChanges();

    const peticion = requests.expectOne('http://localhost:8080/api/solicitudes?estado=PENDIENTE');
    peticion.flush([
      { id: 1, aulaId: 2, docenteId: 3, inicio: 's', fin: 'e', estado: 'PENDIENTE', motivo: 'm', revisadaPorId: null, revisadaEn: null, creadaEn: 'c' },
      { id: 2, aulaId: 2, docenteId: 3, inicio: 's', fin: 'e', estado: 'PENDIENTE', motivo: 'm', revisadaPorId: null, revisadaEn: null, creadaEn: 'c' }
    ]);
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.titulo-grupo')).map(e => e.nativeElement.textContent);
    expect(textos).toContain('Administración');
    const enlaces = f.debugElement
      .queryAll(By.css('a'))
      .map(e => e.nativeElement.getAttribute('href'));
    expect(enlaces).toContain('/aulas');
    expect(enlaces).toContain('/usuarios');
    expect(f.debugElement.query(By.css('.insignia')).nativeElement.textContent.trim()).toBe('2');
  });

  it('colapsa y expande según el estado compartido del shell', () => {
    const f = fixture();
    const shell = TestBed.inject(ShellState);
    f.detectChanges();

    shell.alternarColapso();
    f.detectChanges();
    expect(f.debugElement.query(By.css('.sidebar')).classes['colapsada']).toBe(true);

    shell.alternarColapso();
    f.detectChanges();
    expect(f.debugElement.query(By.css('.sidebar')).classes['colapsada']).toBeUndefined();
  });

  it('la tarjeta del periodo muestra la semana vigente y no rompe fuera de rango', () => {
    const f = fixture();
    f.detectChanges();

    const texto = f.debugElement.query(By.css('.periodo')).nativeElement.textContent;
    if (texto.includes('Semana')) {
      expect(texto).toMatch(/Semana \d+ de 16/);
    } else {
      expect(texto).toContain('16 semanas');
    }
  });
});
