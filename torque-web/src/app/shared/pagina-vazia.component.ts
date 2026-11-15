import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Página provisória: cada módulo troca pela tela real nos próximos prompts. */
@Component({
  selector: 'app-pagina-vazia',
  standalone: true,
  template: `
    <div class="page-head"><h1>{{ titulo }}</h1></div>
    <div class="card vazio">Em construção.</div>
  `,
  styles: `.vazio { padding: 32px; text-align: center; color: var(--torque-texto-suave); }`,
})
export class PaginaVaziaComponent {
  readonly titulo: string = inject(ActivatedRoute).snapshot.data['titulo'] ?? '';
}
