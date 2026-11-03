import { Routes } from '@angular/router';

import { requiereRoles } from './core/auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'aulas' },
  {
    path: 'login',
    loadComponent: () => import('./features/sesion/login-page').then(m => m.LoginPage)
  },
  {
    path: 'aulas',
    canActivate: [requiereRoles()],
    loadComponent: () => import('./features/aulas/presentation/pages/aulas-page').then(m => m.AulasPage)
  },
  {
    path: 'solicitudes',
    canActivate: [requiereRoles('DOCENTE', 'ADMINISTRATIVO', 'ADMINISTRADOR')],
    loadComponent: () =>
      import('./features/solicitudes/presentation/pages/solicitudes-page').then(m => m.SolicitudesPage)
  },
  {
    path: 'aulabot',
    canActivate: [requiereRoles()],
    loadComponent: () => import('./features/aulabot/aulabot-page').then(m => m.AulaBotPage)
  },
  { path: '**', redirectTo: '' }
];
