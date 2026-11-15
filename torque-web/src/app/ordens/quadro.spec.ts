import { agruparPorStatus, diasDesde, emAlerta, haQuantoTempo, infoDoCartao, moeda, situacaoDoCartao } from './quadro';
import { ordem } from './testes-ordens';

describe('regras do quadro', () => {
  const agora = new Date(2026, 10, 15, 14, 0); // 15/11/2026 14:00

  it('deve agrupar por status com as seis colunas, inclusive vazias', () => {
    const grupos = agruparPorStatus([
      ordem('ABERTA'), ordem('ABERTA'), ordem('EM_EXECUCAO'), ordem('CONCLUIDA'),
    ]);

    expect([...grupos.keys()]).toEqual([
      'ABERTA', 'AGUARDANDO_APROVACAO', 'APROVADA', 'EM_EXECUCAO', 'AGUARDANDO_PECA', 'CONCLUIDA',
    ]);
    expect(grupos.get('ABERTA')!.length).toBe(2);
    expect(grupos.get('EM_EXECUCAO')!.length).toBe(1);
    expect(grupos.get('AGUARDANDO_PECA')!.length).toBe(0);
  });

  it('deve deixar de fora os status finais', () => {
    const grupos = agruparPorStatus([ordem('ENTREGUE'), ordem('CANCELADA'), ordem('RECUSADA')]);

    expect([...grupos.values()].every((lista) => lista.length === 0)).toBeTrue();
  });

  it('deve alertar orçamento enviado há mais de 1 dia', () => {
    expect(emAlerta(ordem('AGUARDANDO_APROVACAO', { dataStatus: '2026-11-14T13:59:00' }), agora)).toBeTrue();
    expect(emAlerta(ordem('AGUARDANDO_APROVACAO', { dataStatus: '2026-11-14T14:30:00' }), agora)).toBeFalse();
    expect(emAlerta(ordem('AGUARDANDO_APROVACAO', { dataStatus: '2026-11-15T09:00:00' }), agora)).toBeFalse();
  });

  it('deve alertar sempre a OS aguardando peça e nunca as demais colunas', () => {
    expect(emAlerta(ordem('AGUARDANDO_PECA', { dataStatus: '2026-11-15T13:00:00' }), agora)).toBeTrue();
    expect(emAlerta(ordem('EM_EXECUCAO', { dataStatus: '2026-11-01T08:00:00' }), agora)).toBeFalse();
  });

  it('deve contar dias de calendário', () => {
    expect(diasDesde(new Date(2026, 10, 15, 0, 1), agora)).toBe(0);
    expect(diasDesde(new Date(2026, 10, 14, 23, 59), agora)).toBe(1);
    expect(haQuantoTempo(new Date(2026, 10, 12), agora)).toBe('há 3 dias');
  });

  it('deve mostrar valor ou tempo no status conforme a coluna', () => {
    expect(infoDoCartao(ordem('ABERTA', { dataAbertura: '2026-11-15T08:15:00' }), agora)).toBe('hoje 08:15');
    expect(infoDoCartao(ordem('AGUARDANDO_APROVACAO', { total: 1240 }), agora)).toBe('R$ 1.240,00');
    expect(infoDoCartao(ordem('EM_EXECUCAO', { dataStatus: '2026-11-15T08:40:00' }), agora)).toBe('desde 08:40');
    expect(infoDoCartao(ordem('EM_EXECUCAO', { dataStatus: '2026-11-14T08:40:00' }), agora)).toBe('desde ontem');
    expect(infoDoCartao(ordem('AGUARDANDO_PECA', { dataStatus: '2026-11-12T10:00:00' }), agora)).toBe('há 3 dias');
  });

  it('deve descrever há quanto tempo o orçamento foi enviado', () => {
    expect(situacaoDoCartao(ordem('AGUARDANDO_APROVACAO', { dataStatus: '2026-11-13T10:00:00' }), agora)).toBe('Enviado há 2 dias');
    expect(situacaoDoCartao(ordem('APROVADA'), agora)).toBeNull();
  });

  it('deve formatar moeda em reais com espaço comum', () => {
    expect(moeda(370.5)).toBe('R$ 370,50');
    expect(moeda(null)).toBe('');
  });
});
