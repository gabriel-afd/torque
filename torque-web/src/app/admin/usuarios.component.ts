import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { filter, switchMap } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { ROTULO_PERFIL } from '../core/modelos';
import { confirmar } from '../shared/confirmacao.component';
import { Usuario } from './admin.modelos';
import { AdminService } from './admin.service';
import { UsuarioFormComponent } from './usuario-form.component';
import { comCarregamento } from '../shared/carregamento';
import { ListaPaginada } from '../shared/lista-paginada';

/** Usuários do sistema: cadastro, edição (perfil, senha) e inativação (só ADMINISTRADOR). */
@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatPaginatorModule, MatProgressBarModule],
  template: `
    <div class="page-head">
      <h1>Usuários</h1>
      <button mat-flat-button class="acao-principal" (click)="editar()" data-testid="novo-usuario">
        <mat-icon>add</mat-icon> Novo usuário
      </button>
    </div>
    <div class="card">
      @if (carregando()) { <mat-progress-bar mode="indeterminate" /> }
      <div class="rolagem">
        <table class="tabela-simples">
          <tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Situação</th><th></th></tr>
          @for (u of lista.itens(); track u.id) {
            <tr data-testid="usuario" [class.inativo]="!u.ativo">
              <td>{{ u.nome }}</td>
              <td>{{ u.email }}</td>
              <td>{{ rotuloPerfil[u.perfil] }}</td>
              <td>{{ u.ativo ? 'Ativo' : 'Inativo' }}</td>
              <td class="r acoes">
                <button mat-icon-button (click)="editar(u)" [attr.aria-label]="'Editar ' + u.nome" data-testid="editar"><mat-icon>edit</mat-icon></button>
                @if (u.ativo) {
                  <button mat-icon-button (click)="inativar(u)" [attr.aria-label]="'Inativar ' + u.nome" data-testid="inativar">
                    <mat-icon>person_off</mat-icon>
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">Nenhum usuário.</td></tr>
          }
        </table>
      </div>
      <mat-paginator [length]="lista.total()" [pageIndex]="lista.pagina()" [pageSize]="lista.tamanho()" [pageSizeOptions]="[10, 20, 50]"
                     (page)="mudarPagina($event)" aria-label="Páginas de usuários" />
    </div>
  `,
  styles: `
    .rolagem { overflow-x: auto; }
    .inativo td { color: var(--torque-texto-suave); }
    .acoes { white-space: nowrap; }
  `,
})
export class UsuariosComponent implements OnInit {
  private readonly api = inject(AdminService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);

  readonly lista = new ListaPaginada<Usuario>();
  readonly carregando = signal(false);
  readonly rotuloPerfil = ROTULO_PERFIL;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.usuarios(this.lista.pagina(), this.lista.tamanho()).pipe(comCarregamento(this.carregando)).subscribe({
      next: (resposta) => this.lista.receber(resposta),
      error: (erro) => this.mensagens.erro(erro),
    });
  }

  mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
    this.carregar();
  }

  editar(usuario?: Usuario): void {
    this.dialogo
      .open(UsuarioFormComponent, { data: { usuario }, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.carregar());
  }

  /** A API recusa (422) inativar o último administrador ativo; a mensagem aparece no snackbar. */
  inativar(usuario: Usuario): void {
    confirmar(this.dialogo, {
      titulo: 'Inativar usuário',
      mensagem: `${usuario.nome} não poderá mais entrar no sistema. O histórico das OS continua com o nome dele.`,
      confirmar: 'Inativar',
      perigo: true,
    })
      .pipe(filter(Boolean), switchMap(() => this.api.inativarUsuario(usuario.id)))
      .subscribe({
        next: () => {
          this.mensagens.sucesso(`${usuario.nome} inativado.`);
          this.carregar();
        },
        error: (erro) => this.mensagens.erro(erro),
      });
  }
}
