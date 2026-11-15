import { Component, input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MascaraDirective } from '../shared/mascara.directive';
import { cpfCnpjValidator, formatarDocumento, formatarTelefone, telefoneValidator } from '../shared/validadores';
import { Cliente, ClienteRequest } from './clientes.modelos';

/** Grupo de campos do cliente, reaproveitado no formulário e no cadastro rápido pela placa. */
export function criarFormCliente(fb: FormBuilder, cliente?: Cliente | null) {
  return fb.nonNullable.group({
    nome: [cliente?.nome ?? '', [Validators.required, Validators.maxLength(120)]],
    documento: [formatarDocumento(cliente?.documento), [Validators.required, cpfCnpjValidator]],
    telefone: [formatarTelefone(cliente?.telefone), [Validators.required, telefoneValidator]],
    email: [cliente?.email ?? '', [Validators.email, Validators.maxLength(160)]],
  });
}

export type FormCliente = ReturnType<typeof criarFormCliente>;

export function paraClienteRequest(form: FormCliente): ClienteRequest {
  const valor = form.getRawValue();
  return { nome: valor.nome.trim(), documento: valor.documento, telefone: valor.telefone, email: valor.email.trim() || null };
}

@Component({
  selector: 'app-cliente-campos',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MascaraDirective],
  template: `
    <div class="campos" [formGroup]="form()">
      <mat-form-field class="largo">
        <mat-label>Nome</mat-label>
        <input matInput formControlName="nome" autocomplete="name" />
        @if (form().controls['nome'].hasError('required')) { <mat-error>Informe o nome</mat-error> }
        @if (form().controls['nome'].hasError('api')) { <mat-error>{{ form().controls['nome'].getError('api') }}</mat-error> }
      </mat-form-field>

      <mat-form-field>
        <mat-label>CPF ou CNPJ</mat-label>
        <input matInput formControlName="documento" appMascara="documento" inputmode="numeric" data-testid="documento" />
        @if (form().controls['documento'].hasError('required')) {
          <mat-error>Informe o CPF ou CNPJ</mat-error>
        } @else if (form().controls['documento'].hasError('cpfCnpj')) {
          <mat-error data-testid="erro-documento">CPF/CNPJ inválido</mat-error>
        } @else if (form().controls['documento'].hasError('api')) {
          <mat-error data-testid="erro-documento">{{ form().controls['documento'].getError('api') }}</mat-error>
        }
      </mat-form-field>

      <mat-form-field>
        <mat-label>Telefone (WhatsApp)</mat-label>
        <input matInput formControlName="telefone" appMascara="telefone" inputmode="tel" autocomplete="tel" />
        @if (form().controls['telefone'].hasError('required')) {
          <mat-error>Informe o telefone</mat-error>
        } @else if (form().controls['telefone'].hasError('telefone')) {
          <mat-error>Telefone com DDD (10 ou 11 dígitos)</mat-error>
        } @else if (form().controls['telefone'].hasError('api')) {
          <mat-error>{{ form().controls['telefone'].getError('api') }}</mat-error>
        }
      </mat-form-field>

      <mat-form-field>
        <mat-label>E-mail (opcional)</mat-label>
        <input matInput formControlName="email" type="email" autocomplete="email" />
        @if (form().controls['email'].hasError('email')) { <mat-error>E-mail inválido</mat-error> }
        @if (form().controls['email'].hasError('api')) { <mat-error>{{ form().controls['email'].getError('api') }}</mat-error> }
      </mat-form-field>
    </div>
  `,
  styles: `
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); column-gap: 12px; }
    .largo { grid-column: 1 / -1; }
  `,
})
export class ClienteCamposComponent {
  readonly form = input.required<FormGroup>();
}
