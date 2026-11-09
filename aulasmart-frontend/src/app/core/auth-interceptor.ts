import { HttpInterceptorFn, HttpResponseBase } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

import { AuthService } from './auth-service';
import { apiUrl } from './api-url';

/** Adjunta la credencial Basic de la sesion activa y limpia la sesion al recibir 401. */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const esApi = peticion.url === apiUrl || peticion.url.startsWith(`${apiUrl}/`);
  const autenticada = esApi && authService.sesion() && !peticion.headers.has('Authorization')
    ? peticion.clone({ setHeaders: { Authorization: `Basic ${authService.sesion()!.claveCodificada}` } })
    : peticion;

  return siguiente(autenticada).pipe(
    catchError(error => {
      if (esApi && error instanceof HttpResponseBase && error.status === 401) {
        authService.logout();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
