import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MensagemService } from '../core/mensagem.service';
import { Perfil, ROTULO_PERFIL } from '../core/modelos';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { Usuario } from './admin.modelos';
import { AdminService } from './admin.service';
import { comCarregamento } from '../shared/carregamento';

/** Senha de 8 a 72 caracteres (a API também confere o limite de 72 bytes do BCrypt). */
const VALIDADORES_SENHA = [Validators.minLength(8), Validators.maxLength(72)];

/** Cadastro e edição de usuário (diálogo). Na edição a senha é opcional. Fecha devolvendo o usuário salvo. */
@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ usuario ? 'Editar usuário' : 'Novo usuário' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="salvar()" id="form-usuario" class="campos" novalidate>
        <mat-form-field>
          <mat-label>Nome</mat-label>
          <input matInput formControlName="nome" maxlength="120" data-testid="nome" />
          @if (form.controls.nome.hasError('required')) { <mat-error>Informe o nome</mat-error> }
        </mat-form-field>
        <mat-form-field>
          <mat-label>E-mail</mat-label>
          <input matInput type="email" formControlName="email" maxlength="160" autocomplete="off" data-testid="email" />
          @if (form.controls.email.hasError('required')) { <mat-error>Informe o e-mail</mat-error> }
          @else if (form.controls.email.hasError('email')) { <mat-error>E-mail inválido</mat-error> }
          @if (form.controls.email.hasError('api')) { <mat-error>{{ form.controls.email.getError('api') }}</mat-error> }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Perfil</mat-label>
          <mat-select formControlName="perfil" data-testid="perfil">
            @for (p of perfis; track p) { <mat-option [value]="p">{{ rotuloPerfil[p] }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>{{ usuario ? 'Nova senha (opcional)' : 'Senha' }}</mat-label>
          <input matInput type="password" formControlName="senha" autocomplete="new-password" data-testid="senha" />
          @if (usuario) { <mat-hint>Deixe em branco para manter a senha atual</mat-hint> }
          @if (form.controls.senha.hasError('required')) { <mat-error>Informe a senha</mat-error> }
          @else if (form.controls.senha.hasError('minlength') || form.controls.senha.hasError('maxlength')) {
            <mat-error>A senha deve ter entre 8 e 72 caracteres</mat-error>
          }
          @if (form.controls.senha.hasError('api')) { <mat-error>{{ form.controls.senha.getError('api') }}</mat-error> }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="acao-principal" type="submit" form="form-usuario" [disabled]="salvando()" data-testid="salvar">
        Salvar
      </button>
    </mat-dialog-actions>
  `,
  styles: `.campos { display: grid; gap: 4px; min-width: min(420px, 80vw); }`,
})
export class UsuarioFormComponent {
  readonly usuario = inject<{ usuario?: Usuario } | null>(MAT_DIALOG_DATA, { optional: true })?.usuario ?? null;
  private readonly api = inject(AdminService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<UsuarioFormComponent, Usuario>);

  readonly perfis: Perfil[] = ['ATENDENTE', 'MECANICO', 'ADMINISTRADOR'];
  readonly rotuloPerfil = ROTULO_PERFIL;
  readonly salvando = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    nome: [this.usuario?.nome ?? '', [Validators.required, Validators.maxLength(120)]],
    email: [this.usuario?.email ?? '', [Validators.required, Validators.email, Validators.maxLength(160)]],
    perfil: [this.usuario?.perfil ?? ('ATENDENTE' as Perfil), Validators.required],
    senha: ['', this.usuario ? VALIDADORES_SENHA : [Validators.required, ...VALIDADORES_SENHA]],
  });

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, email, perfil, senha } = this.form.getRawValue();
    const requisicao = { nome: nome.trim(), email: email.trim(), perfil, senha: senha || null };
    (this.usuario ? this.api.atualizarUsuario(this.usuario.id, requisicao) : this.api.cadastrarUsuario(requisicao)).pipe(comCarregamento(this.salvando)).subscribe({
      next: (salvo) => {
        this.mensagens.sucesso('Usuário salvo.');
        this.dialogo.close(salvo);
      },
      error: (erro) => {
        if (!aplicarErrosDaApi(this.form, erro, { email: /e-mail/i })) {
          this.mensagens.erro(erro);
        }
      },
    });
  }
}
