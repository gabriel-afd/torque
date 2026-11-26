import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { CatalogoListaComponent } from './catalogo-lista.component';

/** Catálogo de serviços e peças (só ADMINISTRADOR). */
@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [MatTabsModule, CatalogoListaComponent],
  template: `
    <div class="page-head">
      <div class="titulo">
        <h1>Catálogo</h1>
        <p class="subtitulo">Serviços e peças com os preços usados nos orçamentos.</p>
      </div>
    </div>
    <mat-tab-group animationDuration="0ms">
      <mat-tab label="Serviços"><app-catalogo-lista tipo="servicos" /></mat-tab>
      <mat-tab label="Peças"><ng-template matTabContent><app-catalogo-lista tipo="pecas" /></ng-template></mat-tab>
    </mat-tab-group>
  `,
})
export class CatalogoComponent {}
