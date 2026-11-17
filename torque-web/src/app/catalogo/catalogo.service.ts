import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from '../core/modelos';
import { Peca, Servico } from './catalogo.modelos';

/** Chamadas à API do catálogo de serviços e peças. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  /** Busca de itens ativos para incluir no orçamento. */
  buscarServicos(busca: string, tamanho = 10): Observable<Pagina<Servico>> {
    return this.http.get<Pagina<Servico>>('/api/servicos', { params: parametros(busca, tamanho) });
  }

  buscarPecas(busca: string, tamanho = 10): Observable<Pagina<Peca>> {
    return this.http.get<Pagina<Peca>>('/api/pecas', { params: parametros(busca, tamanho) });
  }
}

function parametros(busca: string, tamanho: number): HttpParams {
  let params = new HttpParams().set('size', tamanho).set('sort', 'descricao,asc');
  if (busca.trim()) {
    params = params.set('busca', busca.trim());
  }
  return params;
}
