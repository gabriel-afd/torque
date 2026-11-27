import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { BuscaPlacaComponent } from './busca-placa.component';
import { CadastroRapidoComponent, DadosCadastroRapido } from './cadastro-rapido.component';
import { VeiculoComDono } from './clientes.modelos';

/**
 * /veiculos: busca rápida por placa (todos os perfis). O balcão também pode cadastrar um veículo direto,
 * sem buscar antes (mesmo cadastro rápido de quando a placa não é encontrada: dono existente ou novo — RN01).
 */
@Component({
  selector: 'app-veiculos-pagina',
  standalone: true,
  imports: [BuscaPlacaComponent, MatButtonModule, MatIconModule],
  template: `
    <div class="page-head">
      <div class="titulo">
        <h1>Veículos</h1>
        <p class="subtitulo">Encontre o veículo pela placa ou cadastre um novo.</p>
      </div>
      @if (podeCadastrar()) {
        <button mat-flat-button class="acao-principal" type="button" (click)="novoVeiculo()" data-testid="novo-veiculo">
          <mat-icon>add</mat-icon> Novo veículo
        </button>
      }
    </div>
    <section class="card conteudo">
      <p class="muted dica">Digite a placa do veículo que chegou à oficina.</p>
      <app-busca-placa />
    </section>
  `,
  styles: `
    .conteudo { padding: 16px; max-width: 760px; }
    .dica { margin-bottom: 12px; }
  `,
})
export class VeiculosPaginaComponent {
  private readonly auth = inject(AuthService);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);

  readonly podeCadastrar = computed(() => this.auth.temPerfil('ATENDENTE', 'ADMINISTRADOR'));

  /** Cadastro com a placa em branco; cadastrado, abre o histórico do veículo (de onde se abre a OS). */
  novoVeiculo(): void {
    const dados: DadosCadastroRapido = { placa: '' };
    this.dialogo
      .open(CadastroRapidoComponent, { data: dados, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((veiculo?: VeiculoComDono) => {
        if (veiculo) {
          this.router.navigate(['/veiculos', veiculo.placa]);
        }
      });
  }
}
