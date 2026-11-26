import { Routes } from '@angular/router';
import { ClienteDetalheComponent } from './cliente-detalhe.component';
import { ClienteFormComponent } from './cliente-form.component';
import { ClienteListaComponent } from './cliente-lista.component';
import { HistoricoVeiculoComponent } from './historico-veiculo.component';
import { VeiculosPaginaComponent } from './veiculos-pagina.component';

export const CLIENTES_ROUTES: Routes = [
  { path: '', component: ClienteListaComponent, title: 'Clientes · Torque' },
  // Cadastro em diálogo sobre a lista; a rota continua existindo para links antigos.
  { path: 'novo', component: ClienteListaComponent, data: { novo: true }, title: 'Novo cliente · Torque' },
  { path: ':id', component: ClienteDetalheComponent, title: 'Cliente · Torque' },
  { path: ':id/editar', component: ClienteFormComponent, title: 'Editar cliente · Torque' },
];

export const VEICULOS_ROUTES: Routes = [
  { path: '', component: VeiculosPaginaComponent, title: 'Veículos · Torque' },
  { path: ':placa', component: HistoricoVeiculoComponent, title: 'Histórico do veículo · Torque' },
];
