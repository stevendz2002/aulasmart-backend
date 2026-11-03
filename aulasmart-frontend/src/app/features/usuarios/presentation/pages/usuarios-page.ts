import { Component, ChangeDetectionStrategy, inject } from '@angular/core';

import { AuthService } from '../../../core/auth-service';

/**
 * Vista de usuarios, exclusiva de ADMINISTRATIVO / ADMINISTRADOR.
 * Solo lectura: el único endpoint de usuarios publicado por el backend es
 * `GET /api/usuarios/whoami` (sesión activa). El directorio completo de
 * usuarios aún no existe en la API y no se simula — dependencia documentada.
 */
@Component({
  selector: 'app-usuarios-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="seccion">
      <h2>Usuarios</h2>
      <p class="ayuda">
        Única información publicada por la API actual: el perfil de la sesión activa
        (<code>GET /api/usuarios/whoami</code>). El listado completo de usuarios del sistema es una
        dependencia pendiente del backend; no se simula.
      </p>

      @if (authService.sesion(); as sesion) {
        <table>
          <thead>
            <tr>
              <th>Campo</th>
              <th>Valor</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Correo</td>
              <td>{{ sesion.correo }}</td>
            </tr>
            <tr>
              <td>Roles</td>
              <td>{{ sesion.roles.join(', ') }}</td>
            </tr>
          </tbody>
        </table>
      } @else {
        <p class="mensaje-error">Sin sesión activa.</p>
      }
    </section>
  `
})
export class UsuariosPage {
  protected readonly authService = inject(AuthService);
}
