import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService } from './auth-service';

/**
 * Guarda de ruta por sesion y por roles. Sin roles exigidos solo requiere
 * sesion activa; con roles, redirige al dashboard cuando el usuario no
 * cuenta con ninguno de ellos.
 */
export const requiereRoles = (...rolesRequeridos: string[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.sesion() === null) {
    return router.parseUrl('/login');
  }
  if (rolesRequeridos.length > 0 && !authService.tieneAlgunRol(rolesRequeridos)) {
    return router.parseUrl('/');
  }
  return true;
};
