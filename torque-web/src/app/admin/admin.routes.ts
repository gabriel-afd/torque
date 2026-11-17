import { Routes } from '@angular/router';
import { PainelComponent } from './painel.component';
import { UsuariosComponent } from './usuarios.component';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'painel' },
  { path: 'painel', component: PainelComponent, title: 'Painel · Torque' },
  { path: 'usuarios', component: UsuariosComponent, title: 'Usuários · Torque' },
];
