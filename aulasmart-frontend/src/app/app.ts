import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);
  protected readonly marca = 'AulaSmart';
  protected readonly anio = new Date().getFullYear();

  protected cerrarSesion(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
