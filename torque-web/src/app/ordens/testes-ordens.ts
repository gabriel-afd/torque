import { OrdemResumo, StatusOS } from './ordens.modelos';

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

export function pagina(ordens: OrdemResumo[]) {
  return { content: ordens, page: { size: 100, number: 0, totalElements: ordens.length, totalPages: 1 } };
}
