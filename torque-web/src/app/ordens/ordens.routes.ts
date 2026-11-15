import { Routes } from '@angular/router';
import { PaginaVaziaComponent } from '../shared/pagina-vazia.component';
import { QuadroComponent } from './quadro.component';

export const ORDENS_ROUTES: Routes = [
  { path: '', component: QuadroComponent, title: 'Quadro de OS · Torque' },
];

/** Tela da OS: a página completa (orçamento, status) entra no prompt 12. */
export const OS_ROUTES: Routes = [
  { path: ':id', component: PaginaVaziaComponent, title: 'Ordem de serviço · Torque', data: { titulo: 'Ordem de serviço' } },
];
