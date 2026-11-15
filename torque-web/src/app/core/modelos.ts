/** Perfis de acesso, iguais ao enum Perfil da API. */
export type Perfil = 'ADMINISTRADOR' | 'ATENDENTE' | 'MECANICO';

export const ROTULO_PERFIL: Record<Perfil, string> = {
  ADMINISTRADOR: 'Administrador',
  ATENDENTE: 'Atendente',
  MECANICO: 'Mecânico',
};

/** Resposta de POST /api/auth/login. */
export interface LoginResponse {
  token: string;
  nome: string;
  perfil: Perfil;
}

/** Usuário logado, guardado em memória e no localStorage. */
export interface Sessao {
  token: string;
  nome: string;
  perfil: Perfil;
  /** Expiração do token (epoch em ms), lida da claim `exp`. */
  expiraEm: number | null;
}

/** Formato padrão de erro da API: { status, erro, mensagem, campos }. */
export interface ErroApi {
  status: number;
  erro: string;
  mensagem: string;
  campos: Record<string, string>;
}

/** Página do Spring Data serializada como DTO (content + page). */
export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}
