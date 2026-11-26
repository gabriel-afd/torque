import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { confirmar } from '../shared/confirmacao.component';
import { CatalogoFormComponent, DadosCatalogoForm } from './catalogo-form.component';
import { ItemCatalogo, ROTULO_TIPO, TipoCatalogo } from './catalogo.modelos';
import { CatalogoService } from './catalogo.service';
import { comCarregamento } from '../shared/carregamento';
import { ListaPaginada } from '../shared/lista-paginada';

/** Lista paginada de serviços ou de peças com busca, inclusão, edição e inativação (só ADMINISTRADOR). */
@Component({
  selector: 'app-catalogo-lista',
  standalone: true,
  imports: [
    ReactiveFormsModule, CurrencyPipe, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatPaginatorModule, MatProgressBarModule,
  ],
  template: `
    <div class="filtros">
      <mat-form-field class="busca" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <mat-label>Buscar {{ tipo() === 'pecas' ? 'por descrição ou código' : 'por descrição' }}</mat-label>
        <input matInput [formControl]="busca" data-testid="busca" />
      </mat-form-field>
      <mat-checkbox [formControl]="incluirInativos" data-testid="incluir-inativos">Mostrar inativos</mat-checkbox>
      <button mat-flat-button class="acao-principal" (click)="editar()" data-testid="novo">
        <mat-icon>add</mat-icon> Novo {{ rotulo().singular }}
      </button>
    </div>

    <div class="card">
      @if (carregando()) { <mat-progress-bar mode="indeterminate" /> }
      <div class="rolagem">
        <table class="tabela-simples" [attr.data-testid]="'tabela-' + tipo()">
          <tr>
            @if (tipo() === 'pecas') { <th>Código</th> }
            <th>Descrição</th><th class="r">{{ tipo() === 'pecas' ? 'Preço de venda' : 'Mão de obra' }}</th><th>Situação</th><th></th>
          </tr>
          @for (item of lista.itens(); track item.id) {
            <tr data-testid="item" [class.inativo]="!item.ativo">
              @if (tipo() === 'pecas') { <td>{{ item.codigo }}</td> }
              <td>{{ item.descricao }}</td>
              <td class="r">{{ item.preco | currency: 'BRL' }}</td>
              <td>{{ item.ativo ? 'Ativo' : 'Inativo' }}</td>
              <td class="r acoes">
                <button mat-icon-button (click)="editar(item)" [attr.aria-label]="'Editar ' + item.descricao" data-testid="editar">
                  <mat-icon>edit</mat-icon>
                </button>
                @if (item.ativo) {
                  <button mat-icon-button (click)="inativar(item)" [attr.aria-label]="'Inativar ' + item.descricao" data-testid="inativar">
                    <mat-icon>block</mat-icon>
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">Nenhum item encontrado.</td></tr>
          }
        </table>
      </div>
      <mat-paginator [length]="lista.total()" [pageIndex]="lista.pagina()" [pageSize]="lista.tamanho()" [pageSizeOptions]="[10, 20, 50]"
                     (page)="mudarPagina($event)" [attr.aria-label]="'Páginas de ' + rotulo().plural" />
    </div>
  `,
  styles: `
    .filtros { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; margin: 12px 0; }
    .busca { flex: 1; min-width: 220px; max-width: 420px; }
    .rolagem { overflow-x: auto; }
    .inativo td { color: var(--torque-texto-suave); }
    .acoes { white-space: nowrap; }
  `,
})
export class CatalogoListaComponent implements OnInit {
  readonly tipo = input.required<TipoCatalogo>();

  private readonly api = inject(CatalogoService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly busca = new FormControl('', { nonNullable: true });
  readonly incluirInativos = new FormControl(false, { nonNullable: true });
  readonly lista = new ListaPaginada<ItemCatalogo>();
  readonly carregando = signal(false);
  readonly rotulo = () => ROTULO_TIPO[this.tipo()];

  ngOnInit(): void {
    this.carregar();
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recomecar());
    this.incluirInativos.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.recomecar());
  }

  carregar(): void {
    this.api.listar(this.tipo(), this.busca.value, this.incluirInativos.value, this.lista.pagina(), this.lista.tamanho()).pipe(comCarregamento(this.carregando)).subscribe({
      next: (resposta) => this.lista.receber(resposta),
      error: (erro) => this.mensagens.erro(erro),
    });
  }

  mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
    this.carregar();
  }

  editar(item?: ItemCatalogo): void {
    const dados: DadosCatalogoForm = { tipo: this.tipo(), item };
    this.dialogo
      .open(CatalogoFormComponent, { data: dados, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.carregar());
  }

  inativar(item: ItemCatalogo): void {
    confirmar(this.dialogo, {
      titulo: `Inativar ${this.rotulo().singular}`,
      mensagem: `"${item.descricao}" deixa de aparecer na busca do orçamento. Orçamentos já feitos não mudam.`,
      confirmar: 'Inativar',
      perigo: true,
    })
      .pipe(filter(Boolean), switchMap(() => this.api.inativar(this.tipo(), item.id)))
      .subscribe({
        next: () => {
          this.mensagens.sucesso(`${item.descricao} inativado.`);
          this.carregar();
        },
        error: (erro) => this.mensagens.erro(erro),
      });
  }

  private recomecar(): void {
    this.lista.recomecar();
    this.carregar();
  }
}
