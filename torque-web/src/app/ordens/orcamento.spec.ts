import { Peca, Servico } from '../catalogo/catalogo.modelos';
import {
  Acao, acoesDisponiveis, alterado, alterarQuantidade, avisoDeDesconto, edicaoCriaNovaVersao, incluirPeca,
  incluirServico, limiteDeDesconto, rascunhoDe, removerPeca, removerServico, requisicaoDe, resumir,
} from './orcamento';
import { StatusOS } from './ordens.modelos';
import { ordemServico } from './testes-ordens';

const servico: Servico = { id: 13, descricao: 'Alinhamento', precoMaoDeObra: 60, ativo: true };
const peca: Peca = { id: 23, codigo: 'FLT-01', descricao: 'Filtro de óleo', precoVenda: 0.1, ativo: true };

describe('orcamento (regras de tela)', () => {
  describe('resumo (RN04)', () => {
    it('deve somar serviços, peças (quantidade × preço) e subtrair o desconto', () => {
      const resumo = resumir(rascunhoDe(ordemServico('AGUARDANDO_APROVACAO')));

      expect(resumo.subtotalServicos).toBe(200);
      expect(resumo.subtotalPecas).toBe(190);
      expect(resumo.desconto).toBe(19.5);
      expect(resumo.total).toBe(370.5);
      expect(resumo.percentualDesconto).toBe(5);
    });

    it('deve recalcular ao incluir, remover e mudar quantidade (CT51)', () => {
      let r = rascunhoDe(ordemServico('ABERTA', {}, { desconto: 0 }));
      r = incluirServico(r, servico);
      expect(resumir(r).subtotalServicos).toBe(260);

      r = incluirPeca(r, peca, 3);
      expect(resumir(r).subtotalPecas).toBe(190.3);

      r = alterarQuantidade(r, 1, 1);
      expect(resumir(r).subtotalPecas).toBe(167.8);

      r = removerServico(removerPeca(r, 0), 0);
      expect(resumir(r)).toEqual(jasmine.objectContaining({ subtotalServicos: 140, subtotalPecas: 22.8, total: 162.8 }));
    });

    it('não deve acumular erro de ponto flutuante com centavos', () => {
      const r = incluirPeca({ servicos: [], pecas: [], desconto: 0 }, peca, 3);
      expect(resumir(r).total).toBe(0.3);
    });

    it('deve dar percentual zero sem itens', () => {
      expect(resumir({ servicos: [], pecas: [], desconto: 0 }).percentualDesconto).toBe(0);
    });
  });

  describe('desconto (RN05)', () => {
    const resumo = resumir(rascunhoDe(ordemServico('ABERTA', {}, { desconto: 0 })));

    it('deve limitar o atendente a 10% do valor dos itens', () => {
      expect(limiteDeDesconto(resumo, 'ATENDENTE')).toBe(39);
      expect(avisoDeDesconto({ ...resumo, desconto: 39 }, 'ATENDENTE')).toBeNull();
      expect(avisoDeDesconto({ ...resumo, desconto: 39.01 }, 'ATENDENTE')).toContain('limite de 10%');
    });

    it('não deve limitar o administrador, salvo acima do valor dos itens', () => {
      expect(limiteDeDesconto(resumo, 'ADMINISTRADOR')).toBeNull();
      expect(avisoDeDesconto({ ...resumo, desconto: 200 }, 'ADMINISTRADOR')).toBeNull();
      expect(avisoDeDesconto({ ...resumo, desconto: 391 }, 'ADMINISTRADOR')).toContain('valor dos itens');
    });
  });

  describe('requisição', () => {
    it('deve referenciar itens salvos por itemId e novos pelo id do catálogo (RN10)', () => {
      let r = rascunhoDe(ordemServico('ABERTA'));
      r = incluirServico(incluirPeca(r, peca, 2), servico);

      const req = requisicaoDe(r, 3);

      expect(req.servicos).toEqual([
        { itemId: 1, servicoId: null }, { itemId: 2, servicoId: null }, { itemId: null, servicoId: 13 },
      ]);
      expect(req.pecas[2]).toEqual({ itemId: null, pecaId: 23, quantidade: 2 });
      expect(req.pecas[0]).toEqual({ itemId: 3, pecaId: null, quantidade: 1 });
      expect(req.versaoOs).toBe(3);
    });

    it('deve copiar descrição e preço do catálogo no item novo', () => {
      const r = incluirServico({ servicos: [], pecas: [], desconto: 0 }, servico);
      expect(r.servicos[0]).toEqual({ itemId: null, servicoId: 13, descricao: 'Alinhamento', preco: 60 });
    });

    it('deve detectar se o rascunho mudou em relação ao salvo', () => {
      const os = ordemServico('ABERTA');
      expect(alterado(rascunhoDe(os), os)).toBeFalse();
      expect(alterado(removerServico(rascunhoDe(os), 0), os)).toBeTrue();
      expect(alterado({ ...rascunhoDe(os), desconto: 10 }, os)).toBeTrue();
    });
  });

  describe('RN07: nova versão', () => {
    it('deve avisar nova versão só com orçamento aprovado e OS aprovada ou em execução', () => {
      expect(edicaoCriaNovaVersao(ordemServico('APROVADA'))).toBeTrue();
      expect(edicaoCriaNovaVersao(ordemServico('EM_EXECUCAO'))).toBeTrue();
      expect(edicaoCriaNovaVersao(ordemServico('AGUARDANDO_APROVACAO'))).toBeFalse();
      expect(edicaoCriaNovaVersao(ordemServico('ABERTA'))).toBeFalse();
    });
  });

  describe('botões por status e perfil', () => {
    const acoes = (status: StatusOS, perfil: 'ATENDENTE' | 'ADMINISTRADOR' | 'MECANICO', atribuida = false): Acao[] =>
      [...acoesDisponiveis(status, { perfil, atribuidaAoUsuario: atribuida })].sort();

    it('ABERTA: atendente edita, envia para aprovação, compartilha e cancela', () => {
      expect(acoes('ABERTA', 'ATENDENTE')).toEqual(['cancelar', 'compartilhar', 'editarOrcamento', 'enviarParaAprovacao']);
    });

    it('AGUARDANDO_APROVACAO: atendente registra aprovação ou recusa', () => {
      expect(acoes('AGUARDANDO_APROVACAO', 'ATENDENTE')).toEqual(['aprovar', 'cancelar', 'compartilhar', 'editarOrcamento', 'recusar']);
    });

    it('APROVADA: só o mecânico atribuído ou o admin inicia o serviço (RN09)', () => {
      expect(acoes('APROVADA', 'ATENDENTE')).not.toContain('iniciar');
      expect(acoes('APROVADA', 'MECANICO', false)).toEqual([]);
      expect(acoes('APROVADA', 'MECANICO', true)).toEqual(['iniciar']);
      expect(acoes('APROVADA', 'ADMINISTRADOR')).toContain('iniciar');
    });

    it('EM_EXECUCAO: mecânico atribuído pede peça ou conclui; atendente pode alterar orçamento ou cancelar', () => {
      expect(acoes('EM_EXECUCAO', 'MECANICO', true)).toEqual(['aguardarPeca', 'concluir']);
      expect(acoes('EM_EXECUCAO', 'ATENDENTE')).toEqual(['cancelar', 'compartilhar', 'editarOrcamento']);
    });

    it('AGUARDANDO_PECA: mecânico retoma; atendente não altera o orçamento', () => {
      expect(acoes('AGUARDANDO_PECA', 'MECANICO', true)).toEqual(['iniciar']);
      expect(acoes('AGUARDANDO_PECA', 'ATENDENTE')).toEqual(['cancelar', 'compartilhar']);
    });

    it('CONCLUIDA: atendente entrega ou cancela; mecânico não tem ações', () => {
      expect(acoes('CONCLUIDA', 'ATENDENTE')).toEqual(['cancelar', 'compartilhar', 'entregar']);
      expect(acoes('CONCLUIDA', 'MECANICO', true)).toEqual([]);
    });

    it('status finais não têm ações (RN08)', () => {
      (['ENTREGUE', 'RECUSADA', 'CANCELADA'] as StatusOS[]).forEach((s) => expect(acoes(s, 'ADMINISTRADOR')).toEqual([]));
    });

    it('só o administrador atribui mecânico', () => {
      expect(acoes('ABERTA', 'ADMINISTRADOR')).toContain('atribuirMecanico');
      expect(acoes('ABERTA', 'ATENDENTE')).not.toContain('atribuirMecanico');
    });
  });
});
