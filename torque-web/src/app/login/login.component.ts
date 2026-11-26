import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { textoDoErro } from '../core/mensagem.service';

export const MENSAGEM_CREDENCIAIS_INVALIDAS = 'E-mail ou senha inválidos';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatIconModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly rota = inject(ActivatedRoute);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });
  readonly erro = signal<string | null>(null);
  readonly enviando = signal(false);
  readonly mostrarSenha = signal(false);
  /** Etapas do fluxo da oficina, no painel de apresentação. */
  readonly etapas = ['Abra a OS', 'Aprove o orçamento', 'Execute e entregue'];

  entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.erro.set(null);
    this.enviando.set(true);
    const { email, senha } = this.form.getRawValue();
    this.auth.login(email.trim(), senha).subscribe({
      next: () => {
        this.enviando.set(false);
        this.router.navigateByUrl(this.destino());
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        const credenciais = erro instanceof HttpErrorResponse && (erro.status === 401 || erro.status === 400);
        this.erro.set(credenciais ? MENSAGEM_CREDENCIAIS_INVALIDAS : textoDoErro(erro));
      },
    });
  }

  /** Volta para a página pedida antes do login (só caminhos internos). */
  private destino(): string {
    const retorno = this.rota.snapshot.queryParamMap.get('retorno');
    return retorno && retorno.startsWith('/') && !retorno.startsWith('//') && !retorno.startsWith('/login')
      ? retorno
      : '/quadro';
  }
}
