export interface Servico {
  id: number;
  descricao: string;
  precoMaoDeObra: number;
  ativo: boolean;
}

export interface Peca {
  id: number;
  codigo: string;
  descricao: string;
  precoVenda: number;
  ativo: boolean;
}

export type TipoCatalogo = 'servicos' | 'pecas';

/** Serviço ou peça na tela de administração do catálogo (`codigo` só existe nas peças). */
export interface ItemCatalogo {
  id: number;
  codigo: string | null;
  descricao: string;
  preco: number;
  ativo: boolean;
}

/** Dados do formulário; o serviço converte para ServicoRequest ou PecaRequest. */
export interface ItemCatalogoRequest {
  codigo: string | null;
  descricao: string;
  preco: number;
}

export const ROTULO_TIPO: Record<TipoCatalogo, { singular: string; plural: string }> = {
  servicos: { singular: 'serviço', plural: 'Serviços' },
  pecas: { singular: 'peça', plural: 'Peças' },
};
