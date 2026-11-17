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
