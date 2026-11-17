import { Routes } from '@angular/router';
import { OrdemDetalheComponent } from './ordem-detalhe.component';
import { QuadroComponent } from './quadro.component';

export const ORDENS_ROUTES: Routes = [
  { path: '', component: QuadroComponent, title: 'Quadro de OS · Torque' },
];

/** Tela da OS: orçamento, ações de status e histórico. */
export const OS_ROUTES: Routes = [
  { path: ':id', component: OrdemDetalheComponent, title: 'Ordem de serviço · Torque' },
];
