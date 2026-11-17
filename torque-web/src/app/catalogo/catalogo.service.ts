import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Pagina } from '../core/modelos';
import { ItemCatalogo, ItemCatalogoRequest, Peca, Servico, TipoCatalogo } from './catalogo.modelos';

/** Chamadas à API do catálogo de serviços e peças. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  /** Busca de itens ativos para incluir no orçamento. */
  buscarServicos(busca: string, tamanho = 10): Observable<Pagina<Servico>> {
    return this.http.get<Pagina<Servico>>('/api/servicos', { params: parametros(busca, 0, tamanho) });
  }

  buscarPecas(busca: string, tamanho = 10): Observable<Pagina<Peca>> {
    return this.http.get<Pagina<Peca>>('/api/pecas', { params: parametros(busca, 0, tamanho) });
  }

  // Administração (escrita só ADMINISTRADOR) -----------------------------------------------------

  listar(tipo: TipoCatalogo, busca: string, incluirInativos: boolean, pagina: number, tamanho: number): Observable<Pagina<ItemCatalogo>> {
    const params = parametros(busca, pagina, tamanho).set('incluirInativos', incluirInativos);
    return this.http
      .get<Pagina<Servico | Peca>>(`/api/${tipo}`, { params })
      .pipe(map((p) => ({ ...p, content: p.content.map(paraItem) })));
  }

  cadastrar(tipo: TipoCatalogo, item: ItemCatalogoRequest): Observable<ItemCatalogo> {
    return this.http.post<Servico | Peca>(`/api/${tipo}`, paraRequest(tipo, item)).pipe(map(paraItem));
  }

  atualizar(tipo: TipoCatalogo, id: number, item: ItemCatalogoRequest): Observable<ItemCatalogo> {
    return this.http.put<Servico | Peca>(`/api/${tipo}/${id}`, paraRequest(tipo, item)).pipe(map(paraItem));
  }

  inativar(tipo: TipoCatalogo, id: number): Observable<ItemCatalogo> {
    return this.http.patch<Servico | Peca>(`/api/${tipo}/${id}/inativar`, null).pipe(map(paraItem));
  }
}

function parametros(busca: string, pagina: number, tamanho: number): HttpParams {
  let params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'descricao,asc');
  if (busca.trim()) {
    params = params.set('busca', busca.trim());
  }
  return params;
}

function paraItem(item: Servico | Peca): ItemCatalogo {
  return 'precoVenda' in item
    ? { id: item.id, codigo: item.codigo, descricao: item.descricao, preco: item.precoVenda, ativo: item.ativo }
    : { id: item.id, codigo: null, descricao: item.descricao, preco: item.precoMaoDeObra, ativo: item.ativo };
}

function paraRequest(tipo: TipoCatalogo, item: ItemCatalogoRequest) {
  return tipo === 'pecas'
    ? { codigo: (item.codigo ?? '').trim(), descricao: item.descricao.trim(), precoVenda: item.preco }
    : { descricao: item.descricao.trim(), precoMaoDeObra: item.preco };
}
