import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MensagemService } from '../core/mensagem.service';
import { PainelMes } from './admin.modelos';
import { AdminService } from './admin.service';
import { comCarregamento } from '../shared/carregamento';

/** Painel do mês (RF16): OS abertas, concluídas, entregues e faturamento, com seletor de mês. */
@Component({
  selector: 'app-painel',
  standalone: true,
  imports: [CurrencyPipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="page-head">
      <h1>Painel do mês</h1>
      <div class="seletor">
        <button mat-icon-button (click)="mudarMes(-1)" aria-label="Mês anterior" data-testid="mes-anterior"><mat-icon>chevron_left</mat-icon></button>
        <span class="mes" data-testid="mes">{{ rotuloMes() }}</span>
        <button mat-icon-button (click)="mudarMes(1)" [disabled]="ehMesAtual()" aria-label="Próximo mês" data-testid="mes-seguinte">
          <mat-icon>chevron_right</mat-icon>
        </button>
      </div>
    </div>
    @if (carregando()) { <mat-progress-bar mode="indeterminate" /> }
    @if (painel(); as p) {
      <div class="cartoes">
        <div class="card cartao"><b data-testid="abertas">{{ p.abertas }}</b><span>OS abertas</span></div>
        <div class="card cartao"><b data-testid="concluidas">{{ p.concluidas }}</b><span>OS concluídas</span></div>
        <div class="card cartao"><b data-testid="entregues">{{ p.entregues }}</b><span>OS entregues</span></div>
        <div class="card cartao destaque"><b data-testid="faturamento">{{ p.faturamento | currency: 'BRL' }}</b><span>Faturamento (OS entregues)</span></div>
      </div>
    }
  `,
  styles: `
    .seletor { display: flex; align-items: center; gap: 4px; margin-left: auto; }
    .mes { min-width: 150px; text-align: center; font-weight: 600; text-transform: capitalize; }
    .cartoes { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
    .cartao { padding: 16px; b { display: block; font-size: 26px; } span { font-size: 12.5px; color: var(--torque-texto-suave); } }
    .destaque b { color: var(--torque-sucesso); }
  `,
})
export class PainelComponent implements OnInit {
  private readonly api = inject(AdminService);
  private readonly mensagens = inject(MensagemService);

  /** Primeiro dia do mês exibido (começa no mês atual). */
  readonly referencia = signal(primeiroDia(new Date()));
  readonly painel = signal<PainelMes | null>(null);
  readonly carregando = signal(false);
  readonly rotuloMes = computed(() => this.referencia().toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }));
  readonly ehMesAtual = computed(() => this.referencia().getTime() >= primeiroDia(new Date()).getTime());

  ngOnInit(): void {
    this.carregar();
  }

  mudarMes(delta: number): void {
    const atual = this.referencia();
    this.referencia.set(new Date(atual.getFullYear(), atual.getMonth() + delta, 1));
    this.carregar();
  }

  private carregar(): void {
    const ref = this.referencia();
    this.api.painel(ref.getFullYear(), ref.getMonth() + 1).pipe(comCarregamento(this.carregando)).subscribe({
      next: (painel) => this.painel.set(painel),
      error: (erro) => this.mensagens.erro(erro),
    });
  }
}

function primeiroDia(data: Date): Date {
  return new Date(data.getFullYear(), data.getMonth(), 1);
}
