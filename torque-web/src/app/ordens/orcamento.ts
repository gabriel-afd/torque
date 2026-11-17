import { Perfil } from '../core/modelos';
import { Peca, Servico } from '../catalogo/catalogo.modelos';
import { OrcamentoRequest, OrdemServico, StatusOS } from './ordens.modelos';

/**
 * Regras de TELA do orçamento: o rascunho que o atendente edita, o resumo recalculado na hora e quais
 * botões aparecem. Quem decide de verdade é a API (422 quando a regra não permite).
 */

/** Serviço no rascunho: `itemId` para item já salvo (preço congelado — RN10), null para item novo. */
export interface ServicoRascunho {
  itemId: number | null;
  servicoId: number;
  descricao: string;
  preco: number;
}

export interface PecaRascunho {
  itemId: number | null;
  pecaId: number;
  descricao: string;
  quantidade: number;
  precoUnitario: number;
}

export interface Rascunho {
  servicos: ServicoRascunho[];
  pecas: PecaRascunho[];
  desconto: number;
}

export interface Resumo {
  subtotalServicos: number;
  subtotalPecas: number;
  subtotal: number;
  desconto: number;
  total: number;
  /** Desconto em % do valor dos itens (0 quando não há itens). */
  percentualDesconto: number;
}

/** RN05: limite de desconto do atendente sobre o valor dos itens. */
export const LIMITE_DESCONTO_ATENDENTE = 0.1;

/** Dinheiro em centavos inteiros para não acumular erro de ponto flutuante. */
const centavos = (valor: number): number => Math.round((Number(valor) || 0) * 100);
const reais = (emCentavos: number): number => emCentavos / 100;

export function rascunhoDe(os: OrdemServico): Rascunho {
  const o = os.orcamento;
  return {
    servicos: o.servicos.map((s) => ({ itemId: s.id, servicoId: s.servicoId, descricao: s.descricao, preco: s.preco })),
    pecas: o.pecas.map((p) => ({
      itemId: p.id, pecaId: p.pecaId, descricao: p.descricao, quantidade: p.quantidade, precoUnitario: p.precoUnitario,
    })),
    desconto: o.desconto,
  };
}

/** RN10: o item novo copia descrição e preço do catálogo no momento da inclusão. */
export function incluirServico(rascunho: Rascunho, servico: Servico): Rascunho {
  const item: ServicoRascunho = { itemId: null, servicoId: servico.id, descricao: servico.descricao, preco: servico.precoMaoDeObra };
  return { ...rascunho, servicos: [...rascunho.servicos, item] };
}

export function incluirPeca(rascunho: Rascunho, peca: Peca, quantidade: number): Rascunho {
  const item: PecaRascunho = {
    itemId: null, pecaId: peca.id, descricao: peca.descricao, quantidade, precoUnitario: peca.precoVenda,
  };
  return { ...rascunho, pecas: [...rascunho.pecas, item] };
}

export function removerServico(rascunho: Rascunho, indice: number): Rascunho {
  return { ...rascunho, servicos: rascunho.servicos.filter((_, i) => i !== indice) };
}

export function removerPeca(rascunho: Rascunho, indice: number): Rascunho {
  return { ...rascunho, pecas: rascunho.pecas.filter((_, i) => i !== indice) };
}

export function alterarQuantidade(rascunho: Rascunho, indice: number, quantidade: number): Rascunho {
  return { ...rascunho, pecas: rascunho.pecas.map((p, i) => (i === indice ? { ...p, quantidade } : p)) };
}

export function subtotalDaPeca(peca: PecaRascunho): number {
  return reais(peca.quantidade * centavos(peca.precoUnitario));
}

/** RN04: total = serviços + Σ(quantidade × preço unitário) das peças − desconto. */
export function resumir(rascunho: Rascunho): Resumo {
  const servicos = rascunho.servicos.reduce((soma, s) => soma + centavos(s.preco), 0);
  const pecas = rascunho.pecas.reduce((soma, p) => soma + p.quantidade * centavos(p.precoUnitario), 0);
  const subtotal = servicos + pecas;
  const desconto = centavos(rascunho.desconto);
  return {
    subtotalServicos: reais(servicos),
    subtotalPecas: reais(pecas),
    subtotal: reais(subtotal),
    desconto: reais(desconto),
    total: reais(subtotal - desconto),
    percentualDesconto: subtotal > 0 ? Math.round((desconto / subtotal) * 1000) / 10 : 0,
  };
}

/** Maior desconto que o perfil pode dar (null = sem limite: ADMINISTRADOR). */
export function limiteDeDesconto(resumo: Resumo, perfil: Perfil | null): number | null {
  if (perfil === 'ADMINISTRADOR') {
    return null;
  }
  return Math.floor(centavos(resumo.subtotal) * LIMITE_DESCONTO_ATENDENTE) / 100;
}

/** Aviso de RN05 para mostrar na tela (a API recusa com 422 de qualquer forma). */
export function avisoDeDesconto(resumo: Resumo, perfil: Perfil | null): string | null {
  if (resumo.desconto > resumo.subtotal) {
    return 'O desconto não pode passar do valor dos itens.';
  }
  const limite = limiteDeDesconto(resumo, perfil);
  if (limite !== null && resumo.desconto > limite) {
    return `Acima do limite de 10% para atendente (máximo ${limite.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}).`;
  }
  return null;
}

export function requisicaoDe(rascunho: Rascunho, versaoOs: number): OrcamentoRequest {
  return {
    servicos: rascunho.servicos.map((s) =>
      s.itemId !== null ? { itemId: s.itemId, servicoId: null } : { itemId: null, servicoId: s.servicoId },
    ),
    pecas: rascunho.pecas.map((p) =>
      p.itemId !== null
        ? { itemId: p.itemId, pecaId: null, quantidade: p.quantidade }
        : { itemId: null, pecaId: p.pecaId, quantidade: p.quantidade },
    ),
    desconto: reais(centavos(rascunho.desconto)),
    versaoOs,
  };
}

/** O rascunho difere do orçamento salvo? */
export function alterado(rascunho: Rascunho, os: OrdemServico): boolean {
  return JSON.stringify(requisicaoDe(rascunho, 0)) !== JSON.stringify(requisicaoDe(rascunhoDe(os), 0));
}

// Ações por status e perfil -------------------------------------------------------------------

export type Acao =
  | 'editarOrcamento'
  | 'enviarParaAprovacao'
  | 'compartilhar'
  | 'aprovar'
  | 'recusar'
  | 'iniciar'
  | 'aguardarPeca'
  | 'concluir'
  | 'entregar'
  | 'cancelar'
  | 'atribuirMecanico';

/** Quem é "balcão" (ATENDENTE ou ADMINISTRADOR) e quem executa (mecânico atribuído ou ADMINISTRADOR — RN09). */
export interface Contexto {
  perfil: Perfil | null;
  /** A OS está atribuída ao usuário logado. */
  atribuidaAoUsuario: boolean;
}

/** Quem pode cada ação: balcão (ATENDENTE/ADMINISTRADOR), executor (mecânico atribuído/ADMINISTRADOR) ou só admin. */
type Executor = 'balcao' | 'executor' | 'admin';

/**
 * Espelho da tabela de transições do CLAUDE.md, só para decidir quais botões mostrar (a API valida de novo
 * cada ação). Status finais (RN08) não aparecem: não têm ações.
 */
const ACOES_POR_STATUS: Partial<Record<StatusOS, [Acao, Executor][]>> = {
  ABERTA: [['editarOrcamento', 'balcao'], ['enviarParaAprovacao', 'balcao']],
  AGUARDANDO_APROVACAO: [['editarOrcamento', 'balcao'], ['aprovar', 'balcao'], ['recusar', 'balcao']],
  APROVADA: [['editarOrcamento', 'balcao'], ['iniciar', 'executor']],
  EM_EXECUCAO: [['editarOrcamento', 'balcao'], ['aguardarPeca', 'executor'], ['concluir', 'executor']],
  AGUARDANDO_PECA: [['iniciar', 'executor']],
  CONCLUIDA: [['entregar', 'balcao']],
};

/** Ações de todo status não final. */
const ACOES_EM_ANDAMENTO: [Acao, Executor][] = [['compartilhar', 'balcao'], ['cancelar', 'balcao'], ['atribuirMecanico', 'admin']];

function pode(executor: Executor, ctx: Contexto): boolean {
  const admin = ctx.perfil === 'ADMINISTRADOR';
  switch (executor) {
    case 'balcao':
      return admin || ctx.perfil === 'ATENDENTE';
    case 'executor':
      return admin || (ctx.perfil === 'MECANICO' && ctx.atribuidaAoUsuario);
    case 'admin':
      return admin;
  }
}

export function acoesDisponiveis(status: StatusOS, ctx: Contexto): Set<Acao> {
  const doStatus = ACOES_POR_STATUS[status];
  if (!doStatus) {
    return new Set();
  }
  return new Set([...doStatus, ...ACOES_EM_ANDAMENTO].filter(([, executor]) => pode(executor, ctx)).map(([acao]) => acao));
}

/** RN07: editar com a OS APROVADA ou EM_EXECUCAO cria nova versão e devolve a OS para aprovação. */
export function edicaoCriaNovaVersao(os: OrdemServico): boolean {
  return os.orcamento.situacao === 'APROVADO' && (os.status === 'APROVADA' || os.status === 'EM_EXECUCAO');
}
