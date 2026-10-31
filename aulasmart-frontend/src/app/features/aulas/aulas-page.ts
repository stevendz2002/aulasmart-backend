import { Component } from '@angular/core';

@Component({
  selector: 'app-aulas-page',
  template: `
    <section>
      <h2>Aulas</h2>
      <p>Consulta del catálogo y disponibilidad por rango de tiempo.</p>
      <p>Se conectará a <code>GET /api/aulas</code> y <code>GET /api/aulas/disponibles</code> en el siguiente incremento.</p>
    </section>
  `
})
export class AulasPage {}
