export type StatusOS =
  | 'ABERTA'
  | 'AGUARDANDO_APROVACAO'
  | 'APROVADA'
  | 'EM_EXECUCAO'
  | 'AGUARDANDO_PECA'
  | 'CONCLUIDA'
  | 'ENTREGUE'
  | 'RECUSADA'
  | 'CANCELADA';

/** Cartão do quadro (GET /api/ordens). Datas no horário local da oficina, sem fuso. */
export interface OrdemResumo {
  id: number;
  numero: number;
  status: StatusOS;
  placa: string;
  veiculo: string;
  clienteId: number;
  cliente: string;
  mecanicoId: number | null;
  mecanico: string | null;
  total: number | null;
  dataAbertura: string;
  dataStatus: string | null;
}

export interface AberturaOsRequest {
  veiculoId: number;
  problemaRelatado: string;
  quilometragem: number | null;
}

/** Só o necessário da resposta de abertura (a resposta completa é uma OrdemServico). */
export interface OrdemCriada {
  id: number;
  numero: number;
}

export type SituacaoOrcamento = 'PENDENTE' | 'APROVADO' | 'RECUSADO' | 'SUBSTITUIDO';

export interface ItemServico {
  id: number;
  servicoId: number;
  descricao: string;
  preco: number;
}

export interface ItemPeca {
  id: number;
  pecaId: number;
  descricao: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
}

export interface Orcamento {
  id: number;
  versao: number;
  situacao: SituacaoOrcamento;
  servicos: ItemServico[];
  pecas: ItemPeca[];
  subtotalServicos: number;
  subtotalPecas: number;
  desconto: number;
  total: number;
  decididoPor: string | null;
  dataDecisao: string | null;
}

export interface HistoricoStatus {
  statusAnterior: StatusOS | null;
  statusNovo: StatusOS;
  usuarioId: number;
  usuario: string;
  dataHora: string;
}

/** GET /api/ordens/{id}. Para o MECANICO, documento/telefone/e-mail do cliente não vêm. */
export interface OrdemServico {
  id: number;
  numero: number;
  status: StatusOS;
  /** Versão da OS (bloqueio otimista): vai em `versaoOs` nas alterações. */
  version: number;
  problemaRelatado: string;
  quilometragem: number | null;
  dataAbertura: string;
  dataEntrega: string | null;
  veiculo: { id: number; placa: string; marca: string; modelo: string; ano: number | null; cor: string | null };
  cliente: { id: number; nome: string; documento?: string; telefone?: string; email?: string | null };
  mecanico: { id: number; nome: string } | null;
  orcamento: Orcamento;
  versoes: { versao: number; situacao: SituacaoOrcamento; total: number }[];
  historico: HistoricoStatus[];
}

/** PUT /api/ordens/{id}/orcamentos/{versao}: estado desejado do orçamento. */
export interface OrcamentoRequest {
  servicos: { itemId: number | null; servicoId: number | null }[];
  pecas: { itemId: number | null; pecaId: number | null; quantidade: number }[];
  desconto: number;
  versaoOs: number;
}

export const ROTULO_STATUS: Record<StatusOS, string> = {
  ABERTA: 'Aberta',
  AGUARDANDO_APROVACAO: 'Aguardando aprovação',
  APROVADA: 'Aprovada',
  EM_EXECUCAO: 'Em execução',
  AGUARDANDO_PECA: 'Aguardando peça',
  CONCLUIDA: 'Concluída',
  ENTREGUE: 'Entregue',
  RECUSADA: 'Recusada',
  CANCELADA: 'Cancelada',
};

/** Colunas do quadro, na ordem do protótipo (os status finais não aparecem). */
export const COLUNAS_QUADRO: { status: StatusOS; titulo: string }[] = [
  { status: 'ABERTA', titulo: 'Aberta' },
  { status: 'AGUARDANDO_APROVACAO', titulo: 'Aguardando aprovação' },
  { status: 'APROVADA', titulo: 'Aprovada' },
  { status: 'EM_EXECUCAO', titulo: 'Em execução' },
  { status: 'AGUARDANDO_PECA', titulo: 'Aguardando peça' },
  { status: 'CONCLUIDA', titulo: 'Concluída' },
];
