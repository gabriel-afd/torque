import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginResponse, Perfil, Sessao } from './modelos';

export const CHAVE_SESSAO = 'torque.sessao';

/**
 * Login, logout e sessão do usuário (token, nome e perfil) em memória e no localStorage.
 * O perfil aqui só decide o que a tela mostra: quem autoriza de verdade é a API.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessao = signal<Sessao | null>(this.restaurar());

  readonly usuario = this.sessao.asReadonly();
  readonly autenticado = computed(() => this.sessao() !== null);
  readonly perfil = computed<Perfil | null>(() => this.sessao()?.perfil ?? null);

  login(email: string, senha: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, senha }).pipe(
      tap((resposta) => this.iniciar(resposta)),
    );
  }

  logout(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE_SESSAO);
    } catch {
      // armazenamento indisponível (modo privado): a sessão em memória já foi limpa
    }
  }

  /** Token válido (não expirado) ou null. */
  token(): string | null {
    const atual = this.sessao();
    if (!atual) {
      return null;
    }
    if (expirada(atual)) {
      this.logout();
      return null;
    }
    return atual.token;
  }

  temPerfil(...perfis: Perfil[]): boolean {
    const perfil = this.perfil();
    return perfil !== null && perfis.includes(perfil);
  }

  private iniciar(resposta: LoginResponse): void {
    const sessao: Sessao = {
      token: resposta.token,
      nome: resposta.nome,
      perfil: resposta.perfil,
      expiraEm: lerExpiracao(resposta.token),
    };
    this.sessao.set(sessao);
    try {
      localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    } catch {
      // sem localStorage a sessão vale só até recarregar a página
    }
  }

  private restaurar(): Sessao | null {
    try {
      const salva = localStorage.getItem(CHAVE_SESSAO);
      if (!salva) {
        return null;
      }
      const sessao = JSON.parse(salva) as Sessao;
      if (!sessao?.token || !sessao.perfil || expirada(sessao)) {
        localStorage.removeItem(CHAVE_SESSAO);
        return null;
      }
      return sessao;
    } catch {
      return null;
    }
  }
}

function expirada(sessao: Sessao): boolean {
  return sessao.expiraEm !== null && sessao.expiraEm <= Date.now();
}

/** Lê a claim `exp` do JWT (sem validar a assinatura: isso é papel da API). */
export function lerExpiracao(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const exp = JSON.parse(json).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}
