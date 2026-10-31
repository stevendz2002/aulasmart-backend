import { Component } from '@angular/core';

@Component({
  selector: 'app-solicitudes-page',
  template: `
    <section>
      <h2>Solicitudes de reserva</h2>
      <p>Registro del docente y bandeja de revisión de los roles administrativos.</p>
      <p>Se conectará a <code>/api/solicitudes</code> y al endpoint de revisión con la id de la Petición en el siguiente incremento.</p>
    </section>
  `
})
export class SolicitudesPage {}
