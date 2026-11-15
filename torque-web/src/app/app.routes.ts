import { Routes } from '@angular/router';
import { anonimoGuard, autenticadoGuard, perfilGuard } from './core/auth.guard';
import { LayoutComponent } from './layout/layout.component';
import { LoginComponent } from './login/login.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [anonimoGuard], title: 'Entrar · Torque' },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [autenticadoGuard],
    canActivateChild: [autenticadoGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'quadro' },
      { path: 'quadro', loadChildren: () => import('./ordens/ordens.routes').then((m) => m.ORDENS_ROUTES) },
      { path: 'ordens', loadChildren: () => import('./ordens/ordens.routes').then((m) => m.OS_ROUTES) },
      {
        path: 'clientes',
        canMatch: [perfilGuard('ATENDENTE', 'ADMINISTRADOR')],
        loadChildren: () => import('./clientes/clientes.routes').then((m) => m.CLIENTES_ROUTES),
      },
      { path: 'veiculos', loadChildren: () => import('./clientes/clientes.routes').then((m) => m.VEICULOS_ROUTES) },
      {
        path: 'catalogo',
        canMatch: [perfilGuard('ADMINISTRADOR')],
        loadChildren: () => import('./catalogo/catalogo.routes').then((m) => m.CATALOGO_ROUTES),
      },
      {
        path: 'admin',
        canMatch: [perfilGuard('ADMINISTRADOR')],
        loadChildren: () => import('./admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
