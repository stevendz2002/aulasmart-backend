import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError, of } from 'rxjs';
import { AuthService } from './auth-service';
import { apiUrl } from './api-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const isApi = request.url === apiUrl || request.url.startsWith(apiUrl + '/');
  const isAuth = request.url.startsWith(apiUrl + '/auth/');
  const token = auth.sesion()?.accessToken;
  const version = auth.sessionVersion;
  if (!isApi || isAuth || !token || request.headers.has('Authorization')) return next(request);
  const withToken = (value: string) => request.clone({ setHeaders: { Authorization: 'Bearer ' + value } });
  return next(withToken(token)).pipe(catchError(error => {
    if (!(error instanceof HttpErrorResponse) || error.status !== 401) return throwError(() => error);
    const current = auth.sesion();
    if (!current || auth.sessionVersion !== version) return throwError(() => error);
    // Otra petición pudo haber renovado mientras esta respuesta viajaba.
    const refresh = current.accessToken !== token ? of(current) : auth.refresh();
    return refresh.pipe(
      switchMap(session => auth.sesion() && auth.sessionVersion === version ? next(withToken(session.accessToken)) : throwError(() => error)),
      catchError(failure => {
        if (failure instanceof HttpErrorResponse && failure.status === 401 && auth.sessionVersion === version) {
          auth.clear();
          void router.navigateByUrl('/login');
        }
        return throwError(() => failure);
      })
    );
  }));
};
