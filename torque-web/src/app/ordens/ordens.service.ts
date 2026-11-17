import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Pagina } from '../core/modelos';
import { AberturaOsRequest, OrcamentoRequest, OrdemCriada, OrdemResumo, OrdemServico, StatusOS } from './ordens.modelos';

export interface FiltroQuadro {
  status: StatusOS[];
  mecanico: number | null;
  busca: string;
}

/** Limite de página da API (spring.data.web.pageable.max-page-size). */
export const TAMANHO_QUADRO = 100;

@Injectable({ providedIn: 'root' })
export class OrdensService {
  private readonly http = inject(HttpClient);

  /** Quadro: a API já restringe o MECANICO às próprias OS. */
  quadro(filtro: FiltroQuadro): Observable<Pagina<OrdemResumo>> {
    let params = new HttpParams().set('size', TAMANHO_QUADRO).set('sort', 'dataAbertura,asc');
    filtro.status.forEach((s) => (params = params.append('status', s)));
    if (filtro.mecanico !== null) {
      params = params.set('mecanico', filtro.mecanico);
    }
    if (filtro.busca.trim()) {
      params = params.set('busca', filtro.busca.trim());
    }
    return this.http.get<Pagina<OrdemResumo>>('/api/ordens', { params });
  }

  abrir(requisicao: AberturaOsRequest): Observable<OrdemCriada> {
    return this.http.post<OrdemCriada>('/api/ordens', requisicao);
  }

  detalhar(id: number): Observable<OrdemServico> {
    return this.http.get<OrdemServico>(`/api/ordens/${id}`);
  }

  /** Orçamento aprovado gera nova versão na API (RN07). */
  alterarOrcamento(id: number, versao: number, requisicao: OrcamentoRequest): Observable<OrdemServico> {
    return this.http.put<OrdemServico>(`/api/ordens/${id}/orcamentos/${versao}`, requisicao);
  }

  aprovar(id: number, versao: number): Observable<OrdemServico> {
    return this.http.post<OrdemServico>(`/api/ordens/${id}/orcamentos/${versao}/aprovacao`, null);
  }

  recusar(id: number, versao: number): Observable<OrdemServico> {
    return this.http.post<OrdemServico>(`/api/ordens/${id}/orcamentos/${versao}/recusa`, null);
  }

  pdf(id: number, versao: number): Observable<Blob> {
    return this.http.get(`/api/ordens/${id}/orcamentos/${versao}/pdf`, { responseType: 'blob' });
  }

  compartilhamento(id: number): Observable<{ linkWhatsapp: string }> {
    return this.http.get<{ linkWhatsapp: string }>(`/api/ordens/${id}/compartilhamento`);
  }

  alterarStatus(id: number, status: StatusOS, versaoOs: number): Observable<OrdemServico> {
    return this.http.patch<OrdemServico>(`/api/ordens/${id}/status`, { status, versaoOs });
  }

  atribuirMecanico(id: number, mecanicoId: number, versaoOs: number): Observable<OrdemServico> {
    return this.http.patch<OrdemServico>(`/api/ordens/${id}/mecanico`, { mecanicoId, versaoOs });
  }

  /** Mecânicos ativos para atribuição (GET /api/usuarios é só do ADMINISTRADOR). */
  mecanicosAtivos(): Observable<{ id: number; nome: string }[]> {
    const params = new HttpParams().set('size', TAMANHO_QUADRO).set('sort', 'nome,asc');
    return this.http
      .get<Pagina<{ id: number; nome: string; perfil: string; ativo: boolean }>>('/api/usuarios', { params })
      .pipe(map((p) => p.content.filter((u) => u.perfil === 'MECANICO' && u.ativo).map(({ id, nome }) => ({ id, nome }))));
  }
}
