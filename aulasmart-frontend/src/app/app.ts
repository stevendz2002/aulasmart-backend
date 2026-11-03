import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Sidebar } from './shared/presentation/sidebar/sidebar';
import { Header } from './shared/presentation/header/header';
import { Footer } from './shared/presentation/footer/footer';

/** Shell de la SPA: menú lateral + cabecera + contenido + pie. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
