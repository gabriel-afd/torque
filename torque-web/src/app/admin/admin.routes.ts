import { Routes } from '@angular/router';
import { PaginaVaziaComponent } from '../shared/pagina-vazia.component';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'painel' },
  { path: 'painel', component: PaginaVaziaComponent, title: 'Painel · Torque', data: { titulo: 'Painel do mês' } },
  { path: 'usuarios', component: PaginaVaziaComponent, title: 'Usuários · Torque', data: { titulo: 'Usuários' } },
];
