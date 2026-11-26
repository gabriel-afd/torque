import {
  agruparPorStatus, diasDesde, doisDigitos, emAlerta, haQuantoTempo, infoDoCartao, moeda, movimentoNoQuadro, resumoDoQuadro,
  situacaoDoCartao, tempoDoCartao, valorDoCartao,
} from './quadro';
import { Contexto } from './orcamento';
import { COLUNAS_QUADRO, StatusOS } from './ordens.modelos';
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

describe('cartão e resumo do quadro (visual Grafite e Lima)', () => {
  const agora = new Date(2026, 10, 15, 14, 0);

  it('deve mostrar o valor só nas colunas em que o tempo não é o principal', () => {
    expect(valorDoCartao(ordem('AGUARDANDO_APROVACAO', { total: 525 }))).toBe('R$ 525,00');
    expect(valorDoCartao(ordem('CONCLUIDA', { total: 458.9 }))).toBe('R$ 458,90');
    expect(valorDoCartao(ordem('ABERTA'))).toBe('');
    expect(valorDoCartao(ordem('EM_EXECUCAO'))).toBe('');
    expect(valorDoCartao(ordem('AGUARDANDO_PECA'))).toBe('');
  });

  it('deve mostrar no rodapé o tempo que importa em cada coluna', () => {
    expect(tempoDoCartao(ordem('ABERTA', { dataAbertura: '2026-11-15T08:15:00' }), agora)).toBe('hoje 08:15');
    expect(tempoDoCartao(ordem('AGUARDANDO_APROVACAO', { dataStatus: '2026-11-13T09:00:00' }), agora)).toBe('Enviado há 2 dias');
    expect(tempoDoCartao(ordem('EM_EXECUCAO', { dataStatus: '2026-11-14T10:00:00' }), agora)).toBe('desde ontem');
    expect(tempoDoCartao(ordem('AGUARDANDO_PECA', { dataStatus: '2026-11-12T10:00:00' }), agora)).toBe('há 3 dias');
    expect(tempoDoCartao(ordem('APROVADA'), agora)).toBeNull();
  });

  it('deve contar o resumo do topo e formatar com dois dígitos', () => {
    const resumo = resumoDoQuadro([
      ordem('ABERTA'), ordem('AGUARDANDO_APROVACAO'), ordem('AGUARDANDO_APROVACAO'), ordem('EM_EXECUCAO'), ordem('CONCLUIDA'),
    ]);

    expect(resumo).toEqual({ total: 5, aguardandoAprovacao: 2, emExecucao: 1, concluidas: 1 });
    expect(doisDigitos(4)).toBe('04');
    expect(doisDigitos(11)).toBe('11');
  });
});

describe('arrastar cartões no quadro (tabela de transições)', () => {
  const atendente: Contexto = { perfil: 'ATENDENTE', atribuidaAoUsuario: false };
  const admin: Contexto = { perfil: 'ADMINISTRADOR', atribuidaAoUsuario: false };
  const mecanicoDaOs: Contexto = { perfil: 'MECANICO', atribuidaAoUsuario: true };
  const outroMecanico: Contexto = { perfil: 'MECANICO', atribuidaAoUsuario: false };

  const PERMITIDOS: [StatusOS, StatusOS, string, Contexto][] = [
    ['ABERTA', 'AGUARDANDO_APROVACAO', 'enviarParaAprovacao', atendente],
    ['AGUARDANDO_APROVACAO', 'APROVADA', 'aprovar', atendente],
    ['APROVADA', 'EM_EXECUCAO', 'iniciar', mecanicoDaOs],
    ['EM_EXECUCAO', 'AGUARDANDO_PECA', 'aguardarPeca', mecanicoDaOs],
    ['EM_EXECUCAO', 'CONCLUIDA', 'concluir', mecanicoDaOs],
    ['AGUARDANDO_PECA', 'EM_EXECUCAO', 'iniciar', mecanicoDaOs],
  ];

  it('deve permitir os movimentos da tabela de transições para quem pode fazê-los', () => {
    for (const [de, para, acao, ctx] of PERMITIDOS) {
      const movimento = movimentoNoQuadro(de, para, ctx);
      expect(movimento).withContext(`${de} → ${para}`).toEqual(jasmine.objectContaining({ permitido: true, acao, destino: para }));
    }
  });

  it('deve permitir ao ADMINISTRADOR todos os movimentos da tabela (RN09)', () => {
    for (const [de, para] of PERMITIDOS) {
      expect(movimentoNoQuadro(de, para, admin)?.permitido).withContext(`${de} → ${para}`).toBeTrue();
    }
  });

  it('deve recusar com mensagem todo movimento fora da tabela de transições', () => {
    const permitidos = new Set(PERMITIDOS.map(([de, para]) => `${de}>${para}`));
    for (const { status: de } of COLUNAS_QUADRO) {
      for (const { status: para } of COLUNAS_QUADRO) {
        if (de === para || permitidos.has(`${de}>${para}`)) {
          continue;
        }
        const movimento = movimentoNoQuadro(de, para, admin);
        expect(movimento?.permitido).withContext(`${de} → ${para}`).toBeFalse();
        expect(movimento && !movimento.permitido && movimento.motivo.length).withContext(`${de} → ${para}`).toBeGreaterThan(10);
      }
    }
  });

  it('não deve deixar pular etapas: de aberta direto para aprovada, em execução ou concluída', () => {
    expect(movimentoNoQuadro('ABERTA', 'APROVADA', admin)).toEqual({
      permitido: false, motivo: 'Só OS aguardando aprovação podem ser aprovadas. Envie o orçamento ao cliente primeiro.',
    });
    expect(movimentoNoQuadro('ABERTA', 'EM_EXECUCAO', admin)).toEqual({
      permitido: false, motivo: 'A OS só entra em execução com o orçamento aprovado pelo cliente.',
    });
    expect(movimentoNoQuadro('ABERTA', 'CONCLUIDA', admin)).toEqual({
      permitido: false, motivo: 'Só OS em execução podem ser concluídas.',
    });
  });

  it('deve orientar a editar o orçamento em vez de voltar a OS aprovada para aprovação (RN07)', () => {
    const movimento = movimentoNoQuadro('APROVADA', 'AGUARDANDO_APROVACAO', atendente);
    expect(movimento?.permitido).toBeFalse();
    expect(movimento && !movimento.permitido && movimento.motivo).toContain('edite os itens');
  });

  it('não deve deixar a OS voltar para aberta', () => {
    expect(movimentoNoQuadro('AGUARDANDO_APROVACAO', 'ABERTA', admin)).toEqual({
      permitido: false, motivo: 'Uma OS não volta para "Aberta".',
    });
  });

  it('deve recusar ao MECANICO os movimentos do balcão', () => {
    expect(movimentoNoQuadro('ABERTA', 'AGUARDANDO_APROVACAO', mecanicoDaOs)).toEqual({
      permitido: false, motivo: 'Só o atendente ou o administrador pode fazer essa mudança.',
    });
    expect(movimentoNoQuadro('AGUARDANDO_APROVACAO', 'APROVADA', mecanicoDaOs)?.permitido).toBeFalse();
  });

  it('deve recusar execução a quem não é o mecânico atribuído nem administrador (RN09)', () => {
    const esperado = { permitido: false as const, motivo: 'Só o mecânico atribuído a esta OS ou o administrador pode fazer essa mudança.' };
    expect(movimentoNoQuadro('APROVADA', 'EM_EXECUCAO', outroMecanico)).toEqual(esperado);
    expect(movimentoNoQuadro('APROVADA', 'EM_EXECUCAO', atendente)).toEqual(esperado);
    expect(movimentoNoQuadro('EM_EXECUCAO', 'CONCLUIDA', atendente)).toEqual(esperado);
  });

  it('não deve fazer nada quando o cartão volta para a própria coluna', () => {
    expect(movimentoNoQuadro('ABERTA', 'ABERTA', admin)).toBeNull();
  });
});
