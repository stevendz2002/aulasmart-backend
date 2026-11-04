import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { App } from './app';
import { AULAS_PROVIDERS } from './features/aulas/infrastructure/aulas-providers';
import { SOLICITUDES_PROVIDERS } from './features/solicitudes/infrastructure/solicitudes-providers';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), ...AULAS_PROVIDERS, ...SOLICITUDES_PROVIDERS]
    }).compileComponents();
    sessionStorage.clear();
  });

  afterEach(() => sessionStorage.clear());

  it('crea la aplicación', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('muestra la marca AulaSmart y la UCEVA en la barra lateral', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const marca = compiled.querySelector('.marca .titulo')?.textContent ?? '';
    expect(marca).toContain('AulaSmart');
    const subtitulo = compiled.querySelector('.marca .subtitulo')?.textContent ?? '';
    expect(subtitulo).toContain('UCEVA');
  });

  it('la barra lateral lleva la tarjeta del período académico', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const periodo = compiled.querySelector('.periodo')?.textContent ?? '';
    expect(periodo).toContain('2026-2');
    expect(periodo).toContain('16');
  });

  it('la cabecera expone el buscador de aulas', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const buscador = compiled.querySelector('app-header input[type=search]');
    expect(buscador).not.toBeNull();
  });
});
