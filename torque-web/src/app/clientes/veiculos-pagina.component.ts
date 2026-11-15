import { Component } from '@angular/core';
import { BuscaPlacaComponent } from './busca-placa.component';

/** /veiculos: busca rápida por placa (todos os perfis); cadastro pelo balcão quando não encontrar. */
@Component({
  selector: 'app-veiculos-pagina',
  standalone: true,
  imports: [BuscaPlacaComponent],
  template: `
    <div class="page-head"><h1>Veículos</h1></div>
    <section class="card conteudo">
      <p class="muted dica">Digite a placa do veículo que chegou à oficina.</p>
      <app-busca-placa />
    </section>
  `,
  styles: `
    .conteudo { padding: 16px; max-width: 760px; }
    .dica { margin-bottom: 12px; }
  `,
})
export class VeiculosPaginaComponent {}
