import { Routes } from '@angular/router';
import { PaginaVaziaComponent } from '../shared/pagina-vazia.component';

export const CATALOGO_ROUTES: Routes = [
  { path: '', component: PaginaVaziaComponent, title: 'Catálogo · Torque', data: { titulo: 'Catálogo' } },
];
