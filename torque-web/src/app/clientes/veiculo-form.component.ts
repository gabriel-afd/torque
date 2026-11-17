import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MensagemService } from '../core/mensagem.service';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { normalizarPlaca, placaValidator } from '../shared/validadores';
import { CONFLITOS_VEICULO, Veiculo, VeiculoRequest } from './clientes.modelos';
import { ClientesService } from './clientes.service';
import { VeiculoCamposComponent } from './veiculo-campos.component';
import { comCarregamento } from '../shared/carregamento';

export interface DadosVeiculoForm {
  clienteId: number;
  veiculo?: Veiculo;
}

const ANO_MAXIMO = new Date().getFullYear() + 1;

export function criarFormVeiculo(fb: FormBuilder, veiculo?: Partial<Veiculo>) {
  return fb.group({
    placa: fb.nonNullable.control(normalizarPlaca(veiculo?.placa), [Validators.required, placaValidator]),
    marca: fb.nonNullable.control(veiculo?.marca ?? '', [Validators.required, Validators.maxLength(60)]),
    modelo: fb.nonNullable.control(veiculo?.modelo ?? '', [Validators.required, Validators.maxLength(80)]),
    ano: fb.control<number | null>(veiculo?.ano ?? null, [Validators.min(1900), Validators.max(ANO_MAXIMO)]),
    cor: fb.nonNullable.control(veiculo?.cor ?? '', [Validators.maxLength(30)]),
  });
}

export type FormVeiculo = ReturnType<typeof criarFormVeiculo>;

export function paraVeiculoRequest(form: FormVeiculo): VeiculoRequest {
  const v = form.getRawValue();
  return { placa: v.placa, marca: v.marca.trim(), modelo: v.modelo.trim(), ano: v.ano || null, cor: v.cor.trim() || null };
}

/** Cadastro e edição de veículo (diálogo aberto a partir do detalhe do cliente). */
@Component({
  selector: 'app-veiculo-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, VeiculoCamposComponent],
  template: `
    <h2 mat-dialog-title>{{ dados.veiculo ? 'Editar veículo' : 'Novo veículo' }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()" novalidate>
      <mat-dialog-content>
        <app-veiculo-campos [form]="form" />
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-stroked-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button class="acao-principal" type="submit" [disabled]="salvando()">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class VeiculoFormComponent {
  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<VeiculoFormComponent, Veiculo>);
  readonly dados = inject<DadosVeiculoForm>(MAT_DIALOG_DATA);

  readonly form = criarFormVeiculo(inject(FormBuilder), this.dados.veiculo);
  readonly salvando = signal(false);

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const requisicao = paraVeiculoRequest(this.form);
    const chamada = this.dados.veiculo
      ? this.api.atualizarVeiculo(this.dados.veiculo.id, requisicao)
      : this.api.cadastrarVeiculo(this.dados.clienteId, requisicao);
    chamada.pipe(comCarregamento(this.salvando)).subscribe({
      next: (veiculo) => {
        this.mensagens.sucesso(this.dados.veiculo ? 'Veículo atualizado.' : 'Veículo cadastrado.');
        this.dialogo.close(veiculo);
      },
      error: (erro) => {
        if (!aplicarErrosDaApi(this.form, erro, CONFLITOS_VEICULO)) {
          this.mensagens.erro(erro);
        }
      },
    });
  }
}
