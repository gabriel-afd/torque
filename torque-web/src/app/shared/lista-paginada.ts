import { signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Pagina } from '../core/modelos';

/** Estado de uma listagem paginada (itens, total, página e tamanho) ligado ao `mat-paginator`. */
export class ListaPaginada<T> {
  readonly itens = signal<T[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly tamanho = signal(20);

  /** Guarda a página que veio da API. */
  receber(resposta: Pagina<T>): void {
    this.itens.set(resposta.content);
    this.total.set(resposta.page.totalElements);
  }

  /** Evento do paginador: nova página e/ou novo tamanho. */
  mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
  }

  /** Filtro mudou: volta para a primeira página. */
  recomecar(): void {
    this.pagina.set(0);
  }
}
