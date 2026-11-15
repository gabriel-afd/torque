import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { MensagemService } from '../core/mensagem.service';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { ClienteCamposComponent, criarFormCliente, paraClienteRequest } from './cliente-campos.component';
import { CONFLITOS_CLIENTE, Cliente } from './clientes.modelos';
import { ClientesService } from './clientes.service';

/** Cadastro (/clientes/novo) e edição (/clientes/:id/editar) de cliente. */
@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressBarModule, ClienteCamposComponent],
  template: `
    <div class="page-head">
      <h1>{{ id() ? 'Editar cliente' : 'Novo cliente' }}</h1>
    </div>
    <form class="card formulario" [formGroup]="form" (ngSubmit)="salvar()" novalidate>
      @if (carregando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <app-cliente-campos [form]="form" />
      <div class="acoes">
        <a mat-stroked-button [routerLink]="id() ? ['/clientes', id()] : ['/clientes']">Cancelar</a>
        <button mat-flat-button class="acao-principal" type="submit" [disabled]="salvando()">Salvar</button>
      </div>
    </form>
  `,
  styles: `
    .formulario { padding: 20px; max-width: 760px; }
    .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
  `,
})
export class ClienteFormComponent implements OnInit {
  /** Parâmetro de rota (withComponentInputBinding). */
  readonly id = input<string>();

  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly router = inject(Router);

  readonly form = criarFormCliente(inject(FormBuilder));
  readonly salvando = signal(false);
  readonly carregando = signal(false);

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.carregando.set(true);
      this.api.buscar(Number(id)).subscribe({
        next: (cliente) => {
          this.form.reset(criarFormCliente(new FormBuilder(), cliente).getRawValue());
          this.carregando.set(false);
        },
        error: (erro) => {
          this.carregando.set(false);
          this.mensagens.erro(erro);
        },
      });
    }
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const id = this.id();
    const requisicao = paraClienteRequest(this.form);
    const chamada = id ? this.api.atualizar(Number(id), requisicao) : this.api.cadastrar(requisicao);
    chamada.subscribe({
      next: (cliente: Cliente) => {
        this.salvando.set(false);
        this.mensagens.sucesso(id ? 'Cliente atualizado.' : 'Cliente cadastrado.');
        this.router.navigate(['/clientes', cliente.id]);
      },
      error: (erro) => {
        this.salvando.set(false);
        if (!aplicarErrosDaApi(this.form, erro, CONFLITOS_CLIENTE)) {
          this.mensagens.erro(erro);
        }
      },
    });
  }
}
