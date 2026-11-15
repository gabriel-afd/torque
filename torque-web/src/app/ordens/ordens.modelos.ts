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

/** Só o necessário da resposta de abertura (o detalhe completo vem no prompt 12). */
export interface OrdemCriada {
  id: number;
  numero: number;
}

/** Colunas do quadro, na ordem do protótipo (os status finais não aparecem). */
export const COLUNAS_QUADRO: { status: StatusOS; titulo: string }[] = [
  { status: 'ABERTA', titulo: 'Aberta' },
  { status: 'AGUARDANDO_APROVACAO', titulo: 'Aguardando aprovação' },
  { status: 'APROVADA', titulo: 'Aprovada' },
  { status: 'EM_EXECUCAO', titulo: 'Em execução' },
  { status: 'AGUARDANDO_PECA', titulo: 'Aguardando peça' },
  { status: 'CONCLUIDA', titulo: 'Concluída' },
];
