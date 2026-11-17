import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, model, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable, catchError, debounceTime, filter, of, switchMap } from 'rxjs';
import { Peca, Servico } from '../catalogo/catalogo.modelos';
import { CatalogoService } from '../catalogo/catalogo.service';
import { MensagemService } from '../core/mensagem.service';
import {
  Rascunho, alterarQuantidade, incluirPeca, incluirServico, removerPeca, removerServico, subtotalDaPeca,
} from './orcamento';

/** Tabelas de serviços e peças do orçamento, com busca no catálogo (edita o rascunho da tela da OS). */
@Component({
  selector: 'app-orcamento-itens',
  standalone: true,
  imports: [
    ReactiveFormsModule, CurrencyPipe, MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatIconModule,
    MatInputModule,
  ],
  templateUrl: './orcamento-itens.component.html',
  styleUrl: './orcamento-itens.component.scss',
})
export class OrcamentoItensComponent implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly mensagens = inject(MensagemService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rascunho = model.required<Rascunho>();
  readonly editavel = input(false);
  readonly versao = input.required<number>();

  readonly servicosEncontrados = signal<Servico[]>([]);
  readonly pecasEncontradas = signal<Peca[]>([]);
  readonly buscaServico = new FormControl<string | Servico>('', { nonNullable: true });
  readonly buscaPeca = new FormControl<string | Peca>('', { nonNullable: true });
  readonly quantidadePeca = new FormControl(1, { nonNullable: true, validators: [Validators.min(1), Validators.max(9999)] });

  readonly subtotalDaPeca = subtotalDaPeca;
  readonly nomeServico = (s: Servico | string | null) => (typeof s === 'string' ? s : s?.descricao ?? '');
  readonly nomePeca = (p: Peca | string | null) => (typeof p === 'string' ? p : p?.descricao ?? '');

  ngOnInit(): void {
    this.autocompletar(this.buscaServico, (t) => this.catalogo.buscarServicos(t), (itens) => this.servicosEncontrados.set(itens));
    this.autocompletar(this.buscaPeca, (t) => this.catalogo.buscarPecas(t), (itens) => this.pecasEncontradas.set(itens));
  }

  adicionarServico(): void {
    const escolhido = this.buscaServico.value;
    if (typeof escolhido === 'string') {
      this.mensagens.aviso('Escolha um serviço da lista do catálogo.');
      return;
    }
    this.rascunho.update((r) => incluirServico(r, escolhido));
    this.buscaServico.setValue('');
  }

  adicionarPeca(): void {
    const escolhida = this.buscaPeca.value;
    if (typeof escolhida === 'string') {
      this.mensagens.aviso('Escolha uma peça da lista do catálogo.');
      return;
    }
    if (this.quantidadePeca.invalid) {
      this.mensagens.aviso('A quantidade deve ser de 1 a 9999.');
      return;
    }
    this.rascunho.update((r) => incluirPeca(r, escolhida, this.quantidadePeca.value));
    this.buscaPeca.setValue('');
    this.quantidadePeca.setValue(1);
  }

  removerServico(indice: number): void {
    this.rascunho.update((r) => removerServico(r, indice));
  }

  removerPeca(indice: number): void {
    this.rascunho.update((r) => removerPeca(r, indice));
  }

  mudarQuantidade(indice: number, valor: string): void {
    const quantidade = Math.trunc(Number(valor));
    if (quantidade >= 1 && quantidade <= 9999) {
      this.rascunho.update((r) => alterarQuantidade(r, indice, quantidade));
    }
  }

  private autocompletar<T>(controle: FormControl<string | T>, buscar: (texto: string) => Observable<{ content: T[] }>,
                           guardar: (itens: T[]) => void): void {
    controle.valueChanges
      .pipe(
        filter((valor): valor is string => typeof valor === 'string'),
        debounceTime(250),
        switchMap((texto) =>
          texto.trim().length >= 2 ? buscar(texto).pipe(catchError(() => of({ content: [] as T[] }))) : of({ content: [] as T[] }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pagina) => guardar(pagina.content));
  }
}
