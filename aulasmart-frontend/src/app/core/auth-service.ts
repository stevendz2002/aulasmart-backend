import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, defer, finalize, firstValueFrom, from, shareReplay, tap } from 'rxjs';
import { apiUrl } from './api-url';

export interface SesionGuardada {
  correo: string;
  nombre: string | null;
  rol: string;
  accessToken: string;
  expiresIn?: number;
}

/** Access solo en memoria; refresh en cookie HttpOnly administrada por el servidor. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private renovacion?: Observable<SesionGuardada>;
  private revision = 0;
  readonly sesion = signal<SesionGuardada | null>(null);
  private readonly options = { withCredentials: true, headers: { 'X-AulaSmart-CSRF': '1' }, timeout: 15000 };
  get sessionVersion(): number { return this.revision; }

  constructor() { sessionStorage.removeItem('aulasmart.sesion'); }

  login(correo: string, password: string): Observable<SesionGuardada> {
    return defer(() => {
      const version = ++this.revision;
      return from(this.exclusive(() => firstValueFrom(this.http.post<SesionGuardada>(apiUrl + '/auth/login', { correo, password }, this.options))))
        .pipe(tap(session => { if (version === this.revision) this.sesion.set(session); }));
    });
  }

  refresh(): Observable<SesionGuardada> {
    if (!this.renovacion) {
      const version = this.revision;
      this.renovacion = defer(() => from(this.exclusive(() => firstValueFrom(
        this.http.post<SesionGuardada>(apiUrl + '/auth/refresh', {}, this.options)
      )))).pipe(
        tap(session => { if (version === this.revision) this.sesion.set(session); }),
        finalize(() => { this.renovacion = undefined; }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.renovacion;
  }

  async restore(): Promise<void> {
    try { await firstValueFrom(this.refresh()); } catch { this.clear(); }
  }

  logout(): Observable<void> {
    return defer(() => {
      ++this.revision;
      return from(this.exclusive(() => firstValueFrom(this.http.post<void>(apiUrl + '/auth/logout', {}, this.options))))
        .pipe(tap(() => this.clear()));
    });
  }

  clear(): void {
    ++this.revision;
    this.sesion.set(null);
    sessionStorage.removeItem('aulasmart.sesion');
  }

  tieneAlgunRol(rolesPermitidos: string[]): boolean {
    const sesion = this.sesion();
    return sesion !== null && rolesPermitidos.includes(sesion.rol);
  }

  // Coordina la cookie compartida entre pestañas cuando Web Locks está disponible.
  private async exclusive<T>(action: () => Promise<T>): Promise<T> {
    return typeof navigator !== 'undefined' && navigator.locks
      ? await navigator.locks.request('aulasmart-auth', action)
      : action();
  }
}
