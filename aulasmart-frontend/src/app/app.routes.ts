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
    loadComponent: () => import('./features/aulas/aulas-page').then(m => m.AulasPage)
  },
  {
    path: 'solicitudes',
    canActivate: [requiereRoles('DOCENTE', 'ADMINISTRATIVO', 'ADMINISTRADOR')],
    loadComponent: () => import('./features/solicitudes/solicitudes-page').then(m => m.SolicitudesPage)
  },
  {
    path: 'bot',
    canActivate: [requiereRoles()],
    loadComponent: () => import('./features/bot/bot-page').then(m => m.BotPage)
  },
  { path: '**', redirectTo: '' }
];
