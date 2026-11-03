import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';

import { AuthService } from '../../../core/auth-service';
import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([])]
    }).compileComponents();
    authService = TestBed.inject(AuthService);
    sessionStorage.clear();
  });

  afterEach(() => sessionStorage.clear());

  function fixture() {
    return TestBed.createComponent(Sidebar);
  }

  it('muestra el grupo General para cualquier sesión (incluso sin sesión)', () => {
    authService.sesion.set(null);
    const f = fixture();
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.grupo .titulo')).map(e => e.nativeElement.textContent);
    expect(textos).toContain('General');
    expect(textos).not.toContain('Administración');
  });

  it('no muestra Administración a un DOCENTE', () => {
    authService.sesion.set({ correo: 'docente@prueba.aulasmart.local', roles: ['DOCENTE'], claveCodificada: 'x' });
    const f = fixture();
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.grupo .titulo')).map(e => e.nativeElement.textContent);
    expect(textos).not.toContain('Administración');
  });

  it('muestra Administración con gestión y usuarios a ADMINISTRATIVO', () => {
    authService.sesion.set({
      correo: 'secretaria@prueba.aulasmart.local',
      roles: ['ADMINISTRATIVO'],
      claveCodificada: 'x'
    });
    const f = fixture();
    f.detectChanges();

    const textos = f.debugElement.queryAll(By.css('.grupo .titulo')).map(e => e.nativeElement.textContent);
    expect(textos).toContain('Administración');
    const enlaces = f.debugElement
      .queryAll(By.css('a'))
      .map(e => e.nativeElement.getAttribute('href'));
    expect(enlaces).toContain('/aulas');
    expect(enlaces).toContain('/usuarios');
  });

  it('el botón de alternar colapsa y expande la barra', () => {
    const f = fixture();
    f.detectChanges();

    f.debugElement.query(By.css('.alternar')).nativeElement.click();
    f.detectChanges();
    expect(f.debugElement.query(By.css('.sidebar')).classes['colapsada']).toBe(true);

    f.debugElement.query(By.css('.alternar')).nativeElement.click();
    f.detectChanges();
    expect(f.debugElement.query(By.css('.sidebar')).classes['colapsada']).toBeUndefined();
  });
});
