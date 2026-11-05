import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { apiUrl } from './api-url';

interface PerfilWhoami {
  correo: string;
  /** Nombre real de la cuenta (tabla usuario); null si no se resuelve. */
  nombre: string | null;
  roles: string[];
}

interface SesionGuardada {
  correo: string;
  nombre: string | null;
  roles: string[];
  claveCodificada: string;
}

const LLAVE_SESION = 'aulasmart.sesion';

/** Sesion activa de la SPA: no hay sesion en el servidor (Basic en cada peticion). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly sesion = signal<SesionGuardada | null>(AuthService.hidratar());

  login(correo: string, password: string): Observable<SesionGuardada> {
    const claveCodificada = btoa(`${correo}:${password}`);
    const headers = { Authorization: `Basic ${claveCodificada}` };
    return this.http.get<PerfilWhoami>(`${apiUrl}/usuarios/whoami`, { headers }).pipe(
      map(perfil => ({ ...perfil, claveCodificada })),
      tap(sesion => {
        sessionStorage.setItem(LLAVE_SESION, JSON.stringify(sesion));
        this.sesion.set(sesion);
      })
    );
  }

  logout(): void {
    sessionStorage.removeItem(LLAVE_SESION);
    this.sesion.set(null);
  }

  tieneAlgunRol(rolesPermitidos: string[]): boolean {
    const sesion = this.sesion();
    return sesion !== null && rolesPermitidos.some(rol => sesion.roles.includes(rol));
  }

  private static hidratar(): SesionGuardada | null {
    const guardada = sessionStorage.getItem(LLAVE_SESION);
    return guardada === null ? null : (JSON.parse(guardada) as SesionGuardada);
  }
}
