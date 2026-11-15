import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from '../core/modelos';
import { Cliente, ClienteDetalhe, ClienteRequest, Veiculo, VeiculoComDono, VeiculoRequest } from './clientes.modelos';

/** Chamadas à API de clientes e veículos. */
@Injectable({ providedIn: 'root' })
export class ClientesService {
  private readonly http = inject(HttpClient);

  listar(busca: string, pagina: number, tamanho: number): Observable<Pagina<Cliente>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'nome,asc');
    if (busca.trim()) {
      params = params.set('busca', busca.trim());
    }
    return this.http.get<Pagina<Cliente>>('/api/clientes', { params });
  }

  buscar(id: number): Observable<ClienteDetalhe> {
    return this.http.get<ClienteDetalhe>(`/api/clientes/${id}`);
  }

  cadastrar(cliente: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>('/api/clientes', cliente);
  }

  atualizar(id: number, cliente: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`/api/clientes/${id}`, cliente);
  }

  inativar(id: number): Observable<Cliente> {
    return this.http.patch<Cliente>(`/api/clientes/${id}/inativar`, null);
  }

  cadastrarVeiculo(clienteId: number, veiculo: VeiculoRequest): Observable<Veiculo> {
    return this.http.post<Veiculo>(`/api/clientes/${clienteId}/veiculos`, veiculo);
  }

  atualizarVeiculo(id: number, veiculo: VeiculoRequest): Observable<Veiculo> {
    return this.http.put<Veiculo>(`/api/veiculos/${id}`, veiculo);
  }

  /** 404 quando a placa não está cadastrada. */
  buscarPorPlaca(placa: string): Observable<VeiculoComDono> {
    return this.http.get<VeiculoComDono>('/api/veiculos', { params: { placa } });
  }
}
