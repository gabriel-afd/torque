import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from '../core/modelos';
import { AberturaOsRequest, OrdemCriada, OrdemResumo, StatusOS } from './ordens.modelos';

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
}
