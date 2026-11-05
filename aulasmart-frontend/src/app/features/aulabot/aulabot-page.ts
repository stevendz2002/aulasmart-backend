import { Component } from '@angular/core';

@Component({
  selector: 'app-aulabot-page',
  template: `
    <section class="seccion">
      <h2>AulaBot</h2>
      <p class="ayuda">Asistente conversacional para consultar disponibilidad de aulas en lenguaje natural.</p>
      <p class="mensaje-info">
        El módulo chatbot del backend aún no está publicado en la API real. Esta interfaz se
        activará contra <code>SendChatMessageUseCase</code> cuando el endpoint exista: no se
        simulan respuestas artificiales.
      </p>
    </section>
  `
})
export class AulaBotPage {}
