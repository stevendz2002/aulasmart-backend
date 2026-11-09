import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UsuariosPage } from './usuarios-page';
import { AuthService } from '../../../../core/auth-service';
import { CREATE_USER_ACCOUNT_USE_CASE, GET_USERS_USE_CASE, UPDATE_USER_ACCOUNT_USE_CASE } from '../../application/users-di';
import { User } from '../../domain/entities/user';

describe('Usuarios con rol único', () => {
  const usuario: User = { id: 1, fullName: 'Ana', email: 'ana@example.test', active: true, role: 'ADMINISTRADOR', createdAt: '' };
  const crear = vi.fn();
  const editar = vi.fn();
  const sesion = signal({ correo: usuario.email, nombre: usuario.fullName, rol: 'ADMINISTRADOR', claveCodificada: '' });

  beforeEach(() => {
    crear.mockReset().mockReturnValue(of({ ...usuario, role: 'ESTUDIANTE' }));
    editar.mockReset().mockReturnValue(of({ ...usuario, role: 'DOCENTE' }));
    sesion.set({ correo: usuario.email, nombre: usuario.fullName, rol: 'ADMINISTRADOR', claveCodificada: '' });
    TestBed.configureTestingModule({ imports: [UsuariosPage], providers: [
      { provide: AuthService, useValue: { sesion } },
      { provide: GET_USERS_USE_CASE, useValue: { execute: () => of([usuario]) } },
      { provide: CREATE_USER_ACCOUNT_USE_CASE, useValue: { execute: crear } },
      { provide: UPDATE_USER_ACCOUNT_USE_CASE, useValue: { execute: editar } }
    ] });
  });

  it('solo permite una selección y envía un rol al crear', () => {
    const fixture = TestBed.createComponent(UsuariosPage);
    fixture.detectChanges();
    fixture.componentInstance['cambiarPestana']('crear');
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const radios = root.querySelectorAll<HTMLInputElement>('input[name="rol-creacion"]');
    radios[3].click();
    fixture.detectChanges();
    expect(root.querySelectorAll('input[name="rol-creacion"]:checked').length).toBe(1);
    root.querySelector<HTMLInputElement>('#nombre')!.value = 'Ana';
    root.querySelector<HTMLInputElement>('#correo')!.value = 'ana@example.test';
    root.querySelector<HTMLInputElement>('#clave')!.value = 'claveSegura1';
    root.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(crear).toHaveBeenCalledWith({ fullName: 'Ana', email: 'ana@example.test', password: 'claveSegura1', role: 'ESTUDIANTE' });
  });

  it('edita el rol singular y actualiza permisos de la sesión propia', () => {
    const fixture = TestBed.createComponent(UsuariosPage);
    fixture.detectChanges();
    fixture.componentInstance['iniciarEdicion'](usuario);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelectorAll<HTMLInputElement>('input[name="rol-edicion"]')[2].click();
    fixture.detectChanges();
    root.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(editar).toHaveBeenCalledWith(1, { fullName: 'Ana', role: 'DOCENTE' });
    expect(sesion().rol).toBe('DOCENTE');
  });
});
