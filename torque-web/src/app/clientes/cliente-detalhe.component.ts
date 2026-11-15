import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { MensagemService } from '../core/mensagem.service';
import { formatarDocumento, formatarTelefone } from '../shared/validadores';
import { ClienteDetalhe, Veiculo } from './clientes.modelos';
import { ClientesService } from './clientes.service';
import { DadosVeiculoForm, VeiculoFormComponent } from './veiculo-form.component';

/** Dados do cliente e seus veículos (/clientes/:id). */
@Component({
  selector: 'app-cliente-detalhe',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './cliente-detalhe.component.html',
  styleUrl: './cliente-detalhe.component.scss',
})
export class ClienteDetalheComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);

  readonly cliente = signal<ClienteDetalhe | null>(null);
  readonly carregando = signal(false);
  readonly documento = formatarDocumento;
  readonly telefone = formatarTelefone;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.api.buscar(Number(this.id())).subscribe({
      next: (cliente) => {
        this.cliente.set(cliente);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.mensagens.erro(erro);
      },
    });
  }

  abrirVeiculo(veiculo?: Veiculo): void {
    const dados: DadosVeiculoForm = { clienteId: Number(this.id()), veiculo };
    this.dialogo
      .open(VeiculoFormComponent, { data: dados, width: '560px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((salvo?: Veiculo) => {
        if (salvo) {
          this.carregar();
        }
      });
  }

  inativar(): void {
    const cliente = this.cliente();
    if (!cliente || !confirm(`Inativar ${cliente.nome}? O cliente deixa de aparecer como ativo.`)) {
      return;
    }
    this.api.inativar(cliente.id).subscribe({
      next: () => {
        this.mensagens.sucesso('Cliente inativado.');
        this.carregar();
      },
      error: (erro) => this.mensagens.erro(erro),
    });
  }
}
