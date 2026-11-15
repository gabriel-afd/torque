import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, switchMap, of, catchError, map } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MensagemService } from '../core/mensagem.service';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { formatarDocumento } from '../shared/validadores';
import { ClienteCamposComponent, criarFormCliente, paraClienteRequest } from './cliente-campos.component';
import { CONFLITOS_CLIENTE, CONFLITOS_VEICULO, Cliente, Veiculo, VeiculoComDono } from './clientes.modelos';
import { ClientesService } from './clientes.service';
import { VeiculoCamposComponent } from './veiculo-campos.component';
import { criarFormVeiculo, paraVeiculoRequest } from './veiculo-form.component';

export interface DadosCadastroRapido {
  placa: string;
}

/**
 * Placa não encontrada: cadastra o veículo para um cliente existente ou para um cliente novo, numa tela só.
 * Fecha devolvendo o veículo com o dono (mesmo formato da busca por placa).
 */
@Component({
  selector: 'app-cadastro-rapido',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatIconModule,
    MatInputModule, ClienteCamposComponent, VeiculoCamposComponent,
  ],
  templateUrl: './cadastro-rapido.component.html',
  styleUrl: './cadastro-rapido.component.scss',
})
export class CadastroRapidoComponent {
  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<CadastroRapidoComponent, VeiculoComDono>);
  private readonly fb = inject(FormBuilder);
  readonly dados = inject<DadosCadastroRapido>(MAT_DIALOG_DATA);

  readonly modo = signal<'existente' | 'novo'>('existente');
  readonly buscaCliente = new FormControl('', { nonNullable: true });
  readonly encontrados = signal<Cliente[]>([]);
  readonly selecionado = signal<Cliente | null>(null);
  readonly formCliente = criarFormCliente(this.fb);
  readonly formVeiculo = criarFormVeiculo(this.fb, { placa: this.dados.placa });
  readonly salvando = signal(false);
  readonly documento = formatarDocumento;

  constructor() {
    this.buscaCliente.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((termo) =>
          termo.trim().length < 2
            ? of([])
            : this.api.listar(termo, 0, 5).pipe(
                map((pagina) => pagina.content.filter((c) => c.ativo)),
                catchError(() => of([])),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((clientes) => this.encontrados.set(clientes));
  }

  escolher(cliente: Cliente): void {
    this.selecionado.set(cliente);
  }

  salvar(): void {
    const novo = this.modo() === 'novo';
    if (novo && this.formCliente.invalid) {
      this.formCliente.markAllAsTouched();
    }
    if (this.formVeiculo.invalid) {
      this.formVeiculo.markAllAsTouched();
    }
    if ((novo && this.formCliente.invalid) || this.formVeiculo.invalid) {
      return;
    }
    if (!novo && !this.selecionado()) {
      this.mensagens.aviso('Escolha o cliente dono do veículo ou cadastre um cliente novo.');
      return;
    }

    this.salvando.set(true);
    if (novo) {
      this.api.cadastrar(paraClienteRequest(this.formCliente)).subscribe({
        next: (cliente) => {
          // a partir daqui o cliente existe: se o veículo falhar, não cadastra o cliente de novo
          this.selecionado.set(cliente);
          this.modo.set('existente');
          this.cadastrarVeiculo(cliente);
        },
        error: (erro) => {
          this.salvando.set(false);
          if (!aplicarErrosDaApi(this.formCliente, erro, CONFLITOS_CLIENTE)) {
            this.mensagens.erro(erro);
          }
        },
      });
    } else {
      this.cadastrarVeiculo(this.selecionado()!);
    }
  }

  private cadastrarVeiculo(cliente: Cliente): void {
    this.api.cadastrarVeiculo(cliente.id, paraVeiculoRequest(this.formVeiculo)).subscribe({
      next: (veiculo: Veiculo) => {
        this.salvando.set(false);
        this.mensagens.sucesso(`Veículo ${veiculo.placa} cadastrado.`);
        this.dialogo.close({
          id: veiculo.id, placa: veiculo.placa, marca: veiculo.marca, modelo: veiculo.modelo, ano: veiculo.ano,
          cor: veiculo.cor,
          dono: {
            id: cliente.id, nome: cliente.nome, documento: cliente.documento, telefone: cliente.telefone,
            email: cliente.email, ativo: cliente.ativo,
          },
        });
      },
      error: (erro) => {
        this.salvando.set(false);
        if (!aplicarErrosDaApi(this.formVeiculo, erro, CONFLITOS_VEICULO)) {
          this.mensagens.erro(erro);
        }
      },
    });
  }
}
