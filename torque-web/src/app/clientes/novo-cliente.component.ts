import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MensagemService } from '../core/mensagem.service';
import { comCarregamento } from '../shared/carregamento';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { ClienteCamposComponent, criarFormCliente, paraClienteRequest } from './cliente-campos.component';
import { CONFLITOS_CLIENTE, Cliente } from './clientes.modelos';
import { ClientesService } from './clientes.service';

/** Cadastro de cliente em diálogo (como o de usuário). Fecha devolvendo o cliente cadastrado. */
@Component({
  selector: 'app-novo-cliente',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, ClienteCamposComponent],
  template: `
    <h2 mat-dialog-title>Novo cliente</h2>
    <mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="salvar()" id="form-novo-cliente" novalidate>
        <app-cliente-campos [form]="form" />
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="acao-principal" type="submit" form="form-novo-cliente" [disabled]="salvando()"
              data-testid="salvar-cliente">Salvar</button>
    </mat-dialog-actions>
  `,
})
export class NovoClienteComponent {
  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<NovoClienteComponent, Cliente>);

  readonly form = criarFormCliente(inject(FormBuilder));
  readonly salvando = signal(false);

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.cadastrar(paraClienteRequest(this.form)).pipe(comCarregamento(this.salvando)).subscribe({
      next: (cliente) => {
        this.mensagens.sucesso('Cliente cadastrado.');
        this.dialogo.close(cliente);
      },
      error: (erro) => {
        if (!aplicarErrosDaApi(this.form, erro, CONFLITOS_CLIENTE)) {
          this.mensagens.erro(erro);
        }
      },
    });
  }
}
