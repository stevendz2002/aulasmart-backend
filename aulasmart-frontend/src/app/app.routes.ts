import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'aulas' },
  {
    path: 'aulas',
    loadComponent: () => import('./features/aulas/aulas-page').then(m => m.AulasPage)
  },
  {
    path: 'solicitudes',
    loadComponent: () => import('./features/solicitudes/solicitudes-page').then(m => m.SolicitudesPage)
  },
  {
    path: 'bot',
    loadComponent: () => import('./features/bot/bot-page').then(m => m.BotPage)
  },
  { path: '**', redirectTo: '' }
];
