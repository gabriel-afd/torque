import { Routes } from '@angular/router';
import { PaginaVaziaComponent } from '../shared/pagina-vazia.component';

export const CLIENTES_ROUTES: Routes = [
  { path: '', component: PaginaVaziaComponent, title: 'Clientes · Torque', data: { titulo: 'Clientes' } },
];

export const VEICULOS_ROUTES: Routes = [
  { path: '', component: PaginaVaziaComponent, title: 'Veículos · Torque', data: { titulo: 'Veículos' } },
];
