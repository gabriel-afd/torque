import { Routes } from '@angular/router';
import { PaginaVaziaComponent } from '../shared/pagina-vazia.component';

export const ORDENS_ROUTES: Routes = [
  { path: '', component: PaginaVaziaComponent, title: 'Quadro de OS · Torque', data: { titulo: 'Quadro de OS' } },
];
