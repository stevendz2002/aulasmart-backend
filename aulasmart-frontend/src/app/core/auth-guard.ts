import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService } from './auth-service';

/**
 * Guarda de ruta por sesion y por roles. Sin roles exigidos solo requiere
 * sesion activa; con roles, redirige al dashboard cuando el usuario no
 * cuenta con ninguno de ellos.
 */
export const requiereRoles = (...rolesRequeridos: string[]): CanActivateFn => (_ruta, estado) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.sesion() === null) {
    return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  }
  if (rolesRequeridos.length > 0 && !authService.tieneAlgunRol(rolesRequeridos)) {
    return router.parseUrl('/');
  }
  return true;
};

export function esRutaPrivada(url: string): boolean {
  return /^\/(solicitudes|aulas|usuarios)(?:[/?#;]|$)/.test(url);
}
