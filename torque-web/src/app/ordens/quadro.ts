import { Acao, Contexto, acoesDisponiveis } from './orcamento';
import { COLUNAS_QUADRO, OrdemResumo, StatusOS } from './ordens.modelos';

const UM_DIA_MS = 24 * 60 * 60 * 1000;

/** Cartões agrupados por status, com todas as colunas do quadro (mesmo vazias). */
export function agruparPorStatus(ordens: OrdemResumo[]): Map<StatusOS, OrdemResumo[]> {
  const grupos = new Map<StatusOS, OrdemResumo[]>(COLUNAS_QUADRO.map((c) => [c.status, []]));
  for (const os of ordens) {
    grupos.get(os.status)?.push(os);
  }
  return grupos;
}

/** Quando a OS entrou no status atual (sem histórico, a abertura). */
export function inicioDoStatus(os: OrdemResumo): Date {
  return new Date(os.dataStatus ?? os.dataAbertura);
}

function inicioDoDia(data: Date): number {
  return new Date(data.getFullYear(), data.getMonth(), data.getDate()).getTime();
}

/** Dias de calendário entre a data e hoje (0 = hoje, 1 = ontem). */
export function diasDesde(data: Date, agora: Date): number {
  return Math.round((inicioDoDia(agora) - inicioDoDia(data)) / UM_DIA_MS);
}

function hora(data: Date): string {
  return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

/** "hoje 08:15", "ontem 17:40", "12/11 09:00". */
export function quando(data: Date, agora: Date): string {
  const dias = diasDesde(data, agora);
  if (dias === 0) {
    return `hoje ${hora(data)}`;
  }
  if (dias === 1) {
    return `ontem ${hora(data)}`;
  }
  return data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }) + ` ${hora(data)}`;
}

/** "hoje", "ontem", "há 3 dias". */
export function haQuantoTempo(data: Date, agora: Date): string {
  const dias = diasDesde(data, agora);
  return dias <= 0 ? 'hoje' : dias === 1 ? 'ontem' : `há ${dias} dias`;
}

/**
 * Alerta vermelho do cartão: orçamento enviado há mais de 1 dia (24 h) sem resposta, ou OS parada
 * aguardando peça.
 */
export function emAlerta(os: OrdemResumo, agora: Date): boolean {
  if (os.status === 'AGUARDANDO_PECA') {
    return true;
  }
  return os.status === 'AGUARDANDO_APROVACAO' && agora.getTime() - inicioDoStatus(os).getTime() > UM_DIA_MS;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function moeda(valor: number | null): string {
  return valor === null ? '' : MOEDA.format(valor).replace(/ /g, ' ');
}

/** Texto do canto superior do cartão: valor ou tempo no status, conforme a coluna (protótipo). */
export function infoDoCartao(os: OrdemResumo, agora: Date): string {
  const desde = inicioDoStatus(os);
  switch (os.status) {
    case 'ABERTA':
      return quando(new Date(os.dataAbertura), agora);
    case 'EM_EXECUCAO':
      return diasDesde(desde, agora) === 0 ? `desde ${hora(desde)}` : `desde ${haQuantoTempo(desde, agora).replace('há ', '')}`;
    case 'AGUARDANDO_PECA':
      return haQuantoTempo(desde, agora);
    default:
      return moeda(os.total);
  }
}

/** Linha de baixo do cartão para status em que o tempo importa. */
export function situacaoDoCartao(os: OrdemResumo, agora: Date): string | null {
  if (os.status === 'AGUARDANDO_APROVACAO') {
    return `Enviado ${haQuantoTempo(inicioDoStatus(os), agora)}`;
  }
  if (os.status === 'AGUARDANDO_PECA') {
    return 'Aguardando peça';
  }
  return null;
}

// Arrastar cartões entre colunas ------------------------------------------------------------------

/** Ações que o quadro executa ao soltar um cartão em outra coluna. */
export type AcaoDoQuadro = Extract<Acao, 'enviarParaAprovacao' | 'aprovar' | 'iniciar' | 'aguardarPeca' | 'concluir'>;

export type MovimentoNoQuadro =
  | { permitido: true; acao: AcaoDoQuadro; destino: StatusOS; sucesso: string }
  | { permitido: false; motivo: string };

/**
 * Movimentos de coluna que viram ação na OS (espelho da tabela de transições do CLAUDE.md). A API valida
 * tudo de novo; aqui é só para responder na hora, com uma mensagem clara, o que não pode.
 */
const MOVIMENTOS: Partial<Record<StatusOS, Partial<Record<StatusOS, [AcaoDoQuadro, string]>>>> = {
  ABERTA: { AGUARDANDO_APROVACAO: ['enviarParaAprovacao', 'Orçamento enviado para aprovação do cliente.'] },
  AGUARDANDO_APROVACAO: { APROVADA: ['aprovar', 'Aprovação do cliente registrada.'] },
  APROVADA: { EM_EXECUCAO: ['iniciar', 'Serviço em execução.'] },
  EM_EXECUCAO: {
    AGUARDANDO_PECA: ['aguardarPeca', 'OS aguardando peça.'],
    CONCLUIDA: ['concluir', 'Serviço concluído.'],
  },
  AGUARDANDO_PECA: { EM_EXECUCAO: ['iniciar', 'Serviço retomado.'] },
};

const ACOES_DO_EXECUTOR: AcaoDoQuadro[] = ['iniciar', 'aguardarPeca', 'concluir'];

function titulo(status: StatusOS): string {
  return COLUNAS_QUADRO.find((c) => c.status === status)?.titulo ?? status;
}

/** Por que o movimento não existe na tabela de transições, em linguagem de balcão. */
function motivoDoBloqueio(de: StatusOS, para: StatusOS): string {
  if (para === 'ABERTA') {
    return 'Uma OS não volta para "Aberta".';
  }
  if (para === 'AGUARDANDO_APROVACAO' && (de === 'APROVADA' || de === 'EM_EXECUCAO')) {
    return 'Para mudar o orçamento, abra a OS e edite os itens: ela volta sozinha para aprovação do cliente.';
  }
  if (de === 'CONCLUIDA') {
    return 'OS concluída só pode ser entregue ou cancelada, pela tela da OS.';
  }
  switch (para) {
    case 'APROVADA':
      return 'Só OS aguardando aprovação podem ser aprovadas. Envie o orçamento ao cliente primeiro.';
    case 'EM_EXECUCAO':
      return 'A OS só entra em execução com o orçamento aprovado pelo cliente.';
    case 'AGUARDANDO_PECA':
      return 'Só OS em execução podem ficar aguardando peça.';
    case 'CONCLUIDA':
      return 'Só OS em execução podem ser concluídas.';
    default:
      return `Não é possível mover a OS de "${titulo(de)}" para "${titulo(para)}".`;
  }
}

/**
 * Decide o que fazer quando um cartão sai da coluna `de` e é solto na coluna `para`. `null` quando o cartão
 * voltou para a própria coluna (nada a fazer).
 */
export function movimentoNoQuadro(de: StatusOS, para: StatusOS, ctx: Contexto): MovimentoNoQuadro | null {
  if (de === para) {
    return null;
  }
  const movimento = MOVIMENTOS[de]?.[para];
  if (!movimento) {
    return { permitido: false, motivo: motivoDoBloqueio(de, para) };
  }
  const [acao, sucesso] = movimento;
  if (!acoesDisponiveis(de, ctx).has(acao)) {
    return {
      permitido: false,
      motivo: ACOES_DO_EXECUTOR.includes(acao)
        ? 'Só o mecânico atribuído a esta OS ou o administrador pode fazer essa mudança.'
        : 'Só o atendente ou o administrador pode fazer essa mudança.',
    };
  }
  return { permitido: true, acao, destino: para, sucesso };
}
