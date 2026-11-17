import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from '../core/modelos';
import { PainelMes, Usuario, UsuarioRequest } from './admin.modelos';

/** Usuários e painel do mês (só ADMINISTRADOR). */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  usuarios(pagina: number, tamanho: number): Observable<Pagina<Usuario>> {
    const params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', 'nome,asc');
    return this.http.get<Pagina<Usuario>>('/api/usuarios', { params });
  }

  cadastrarUsuario(usuario: UsuarioRequest): Observable<Usuario> {
    return this.http.post<Usuario>('/api/usuarios', usuario);
  }

  /** Senha vazia vai como null: a API mantém a atual. */
  atualizarUsuario(id: number, usuario: UsuarioRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`/api/usuarios/${id}`, { ...usuario, senha: usuario.senha || null });
  }

  inativarUsuario(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`/api/usuarios/${id}/inativar`, null);
  }

  painel(ano: number, mes: number): Observable<PainelMes> {
    return this.http.get<PainelMes>('/api/relatorios/mes', { params: { ano, mes } });
  }
}
