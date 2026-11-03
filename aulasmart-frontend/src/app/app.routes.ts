import { Routes } from '@angular/router';

import { requiereRoles } from './core/auth-guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [requiereRoles()],
    loadComponent: () =>
      import('./features/dashboard/presentation/pages/dashboard-page').then(m => m.DashboardPage)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/sesion/login-page').then(m => m.LoginPage)
  },
  {
    path: 'consulta',
    canActivate: [requiereRoles()],
    loadComponent: () =>
      import('./features/aulas/presentation/pages/consulta-page').then(m => m.ConsultaPage)
  },
  {
    path: 'aulabot',
    canActivate: [requiereRoles()],
    loadComponent: () => import('./features/aulabot/aulabot-page').then(m => m.AulaBotPage)
  },
  {
    path: 'solicitudes',
    canActivate: [requiereRoles('DOCENTE', 'ADMINISTRATIVO', 'ADMINISTRADOR')],
    loadComponent: () =>
      import('./features/solicitudes/presentation/pages/solicitudes-page').then(m => m.SolicitudesPage)
  },
  {
    path: 'aulas',
    canActivate: [requiereRoles('ADMINISTRATIVO', 'ADMINISTRADOR')],
    loadComponent: () =>
      import('./features/aulas/presentation/pages/gestion-aulas-page').then(m => m.GestionAulasPage)
  },
  { path: '**', redirectTo: '' }
];
