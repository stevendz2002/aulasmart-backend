import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth-interceptor';
import { AULAS_PROVIDERS } from './features/aulas/infrastructure/aulas-providers';
import { SOLICITUDES_PROVIDERS } from './features/solicitudes/infrastructure/solicitudes-providers';
import { USUARIOS_PROVIDERS } from './features/usuarios/infrastructure/users-providers';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    ...AULAS_PROVIDERS,
    ...SOLICITUDES_PROVIDERS,
    ...USUARIOS_PROVIDERS
  ]
};
