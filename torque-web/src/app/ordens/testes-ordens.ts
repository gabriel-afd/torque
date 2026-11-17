import { Orcamento, OrdemResumo, OrdemServico, StatusOS } from './ordens.modelos';

let sequencia = 0;

/** Cartão fictício do quadro. Datas no formato da API (LocalDateTime sem fuso). */
export function ordem(status: StatusOS, extra: Partial<OrdemResumo> = {}): OrdemResumo {
  sequencia++;
  return {
    id: sequencia,
    numero: 1000 + sequencia,
    status,
    placa: 'ABC1D23',
    veiculo: 'Fiat Uno',
    clienteId: 1,
    cliente: 'Joana Fictícia',
    mecanicoId: null,
    mecanico: null,
    total: 190,
    dataAbertura: '2026-11-15T08:15:00',
    dataStatus: '2026-11-15T08:15:00',
    ...extra,
  };
}

export function pagina<T = OrdemResumo>(itens: T[]) {
  return { content: itens, page: { size: 100, number: 0, totalElements: itens.length, totalPages: 1 } };
}

/** OS completa fictícia (GET /api/ordens/{id}) com o orçamento do protótipo. */
export function ordemServico(status: StatusOS, extra: Partial<OrdemServico> = {}, orcamento: Partial<Orcamento> = {}): OrdemServico {
  return {
    id: 41,
    numero: 1041,
    status,
    version: 3,
    problemaRelatado: 'Ruído ao frear e pedal baixo',
    quilometragem: 84320,
    dataAbertura: '2026-11-15T10:58:00',
    dataEntrega: null,
    veiculo: { id: 9, placa: 'NBV8D45', marca: 'Renault', modelo: 'Sandero', ano: 2017, cor: 'prata' },
    cliente: { id: 4, nome: 'Rita Fictícia', documento: '52998224725', telefone: '83998124410', email: null },
    mecanico: null,
    orcamento: {
      id: 70,
      versao: 1,
      situacao: status === 'ABERTA' || status === 'AGUARDANDO_APROVACAO' ? 'PENDENTE' : 'APROVADO',
      servicos: [
        { id: 1, servicoId: 11, descricao: 'Troca de pastilhas dianteiras', preco: 120 },
        { id: 2, servicoId: 12, descricao: 'Sangria do sistema de freio', preco: 80 },
      ],
      pecas: [
        { id: 3, pecaId: 21, descricao: 'Jogo de pastilhas dianteiras', quantidade: 1, precoUnitario: 145, subtotal: 145 },
        { id: 4, pecaId: 22, descricao: 'Fluido de freio DOT 4 (500 ml)', quantidade: 2, precoUnitario: 22.5, subtotal: 45 },
      ],
      subtotalServicos: 200,
      subtotalPecas: 190,
      desconto: 19.5,
      total: 370.5,
      decididoPor: null,
      dataDecisao: null,
      ...orcamento,
    },
    versoes: [{ versao: 1, situacao: 'PENDENTE', total: 370.5 }],
    historico: [
      { statusAnterior: null, statusNovo: 'ABERTA', usuarioId: 2, usuario: 'Ana', dataHora: '2026-11-15T10:58:00' },
    ],
    ...extra,
  };
}
