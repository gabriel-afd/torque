import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { NovaOsComponent } from '../ordens/nova-os.component';
import { OrdemCriada, ROTULO_STATUS } from '../ordens/ordens.modelos';
import { diasDesde } from '../ordens/quadro';
import { formatarTelefone, normalizarPlaca } from '../shared/validadores';
import { OsDoVeiculo, VeiculoComDono } from './clientes.modelos';
import { ClientesService } from './clientes.service';
import { comCarregamento } from '../shared/carregamento';

/** Histórico do veículo (HU09), pensado para celular: docs/prototipos/historico.html. */
@Component({
  selector: 'app-historico-veiculo',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DecimalPipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './historico-veiculo.component.html',
  styleUrl: './historico-veiculo.component.scss',
})
export class HistoricoVeiculoComponent implements OnInit {
  /** Parâmetro :placa da rota. */
  readonly placa = input.required<string>();

  private readonly api = inject(ClientesService);
  private readonly auth = inject(AuthService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);

  readonly veiculo = signal<VeiculoComDono | null>(null);
  readonly ordens = signal<OsDoVeiculo[]>([]);
  readonly totalOs = signal(0);
  readonly carregando = signal(false);
  readonly naoEncontrado = signal(false);

  readonly balcao = computed(() => this.auth.temPerfil('ATENDENTE', 'ADMINISTRADOR'));
  /** Quilometragem da OS mais recente que tem km informada. */
  readonly ultimaKm = computed(() => this.ordens().find((os) => os.quilometragem !== null)?.quilometragem ?? null);
  readonly rotuloStatus = ROTULO_STATUS;
  readonly telefone = formatarTelefone;

  ngOnInit(): void {
    this.api
      .buscarPorPlaca(normalizarPlaca(this.placa()))
      .pipe(
        tap((veiculo) => this.veiculo.set(veiculo)),
        switchMap((veiculo) => this.api.historico(veiculo.id)),
        comCarregamento(this.carregando),
      )
      .subscribe({
        next: (pagina) => {
          this.ordens.set(pagina.content);
          this.totalOs.set(pagina.page.totalElements);
        },
        error: (erro: unknown) => {
          if (erro instanceof HttpErrorResponse && erro.status === 404 && !this.veiculo()) {
            this.naoEncontrado.set(true);
          } else {
            this.mensagens.erro(erro);
          }
        },
      });
  }

  descricaoVeiculo(v: VeiculoComDono): string {
    return [`${v.marca} ${v.modelo}`, v.ano, v.cor].filter(Boolean).join(' · ');
  }

  /** "hoje" ou a data (dd/mm/aaaa). */
  data(os: OsDoVeiculo): string {
    const data = new Date(os.dataAbertura);
    return diasDesde(data, new Date()) === 0 ? 'hoje' : data.toLocaleDateString('pt-BR');
  }

  /** Linha de detalhe: serviços do orçamento ou a situação quando não há serviços/aprovação. */
  detalhe(os: OsDoVeiculo): string {
    if (os.status === 'RECUSADA' && os.total !== null) {
      return `Orçamento de ${os.total.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} recusado pelo cliente`;
    }
    if (os.servicos.length) {
      return os.servicos.join(', ');
    }
    return os.status === 'ABERTA' ? 'Aguardando orçamento' : 'Sem serviços no orçamento';
  }

  novaOs(): void {
    const veiculo = this.veiculo();
    if (!veiculo) {
      return;
    }
    this.dialogo
      .open(NovaOsComponent, { data: { veiculo }, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((criada?: OrdemCriada) => {
        if (criada) {
          this.router.navigate(['/ordens', criada.id]);
        }
      });
  }
}
