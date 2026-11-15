import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { MascaraDirective } from '../shared/mascara.directive';
import { formatarTelefone, normalizarPlaca, placaValidator } from '../shared/validadores';
import { CadastroRapidoComponent, DadosCadastroRapido } from './cadastro-rapido.component';
import { VeiculoComDono } from './clientes.modelos';
import { ClientesService } from './clientes.service';

/**
 * Busca rápida por placa. Achou: emite `encontrado`. Não achou: oferece cadastrar o veículo (e o cliente,
 * se preciso) para ATENDENTE e ADMINISTRADOR. Usada na página de veículos e na abertura de OS.
 */
@Component({
  selector: 'app-busca-placa',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MascaraDirective],
  templateUrl: './busca-placa.component.html',
  styleUrl: './busca-placa.component.scss',
})
export class BuscaPlacaComponent {
  /** Mostra o cartão do veículo encontrado (desligue quando a tela usar o resultado de outro jeito). */
  readonly mostrarResultado = input(true);
  /** Texto do botão de ação ao lado do resultado (ex.: "Abrir OS"); vazio = sem botão. */
  readonly textoAcao = input('');
  readonly encontrado = output<VeiculoComDono>();
  readonly acao = output<VeiculoComDono>();

  private readonly api = inject(ClientesService);
  private readonly auth = inject(AuthService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);

  readonly placa = new FormControl('', { nonNullable: true, validators: [Validators.required, placaValidator] });
  /** O grupo liga o `<form>` ao ReactiveForms (sem ele o ngSubmit não é emitido). */
  readonly form = new FormGroup({ placa: this.placa });
  readonly veiculo = signal<VeiculoComDono | null>(null);
  readonly naoEncontrada = signal<string | null>(null);
  readonly buscando = signal(false);
  readonly podeCadastrar = computed(() => this.auth.temPerfil('ATENDENTE', 'ADMINISTRADOR'));
  readonly telefone = formatarTelefone;

  buscar(): void {
    if (this.placa.invalid) {
      this.placa.markAsTouched();
      return;
    }
    const placa = normalizarPlaca(this.placa.value);
    this.buscando.set(true);
    this.veiculo.set(null);
    this.naoEncontrada.set(null);
    this.api.buscarPorPlaca(placa).subscribe({
      next: (veiculo) => {
        this.buscando.set(false);
        this.selecionar(veiculo);
      },
      error: (erro: unknown) => {
        this.buscando.set(false);
        if (erro instanceof HttpErrorResponse && erro.status === 404) {
          this.naoEncontrada.set(placa);
        } else {
          this.mensagens.erro(erro);
        }
      },
    });
  }

  cadastrar(): void {
    const dados: DadosCadastroRapido = { placa: this.naoEncontrada() ?? normalizarPlaca(this.placa.value) };
    this.dialogo
      .open(CadastroRapidoComponent, { data: dados, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((veiculo?: VeiculoComDono) => {
        if (veiculo) {
          this.naoEncontrada.set(null);
          this.selecionar(veiculo);
        }
      });
  }

  private selecionar(veiculo: VeiculoComDono): void {
    this.veiculo.set(veiculo);
    this.encontrado.emit(veiculo);
  }
}
