import { Perfil } from '../core/modelos';

/** Usuário exposto pela API (nunca traz a senha). */
export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
}

/** Cadastro (senha obrigatória) e edição (senha vazia = mantém a atual). */
export interface UsuarioRequest {
  nome: string;
  email: string;
  senha: string | null;
  perfil: Perfil;
}

/** GET /api/relatorios/mes. `faturamento` = soma das OS entregues no mês. */
export interface PainelMes {
  ano: number;
  mes: number;
  abertas: number;
  concluidas: number;
  entregues: number;
  faturamento: number;
}
