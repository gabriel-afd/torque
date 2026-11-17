import { authFalso, mensagensFalsas, provideTesteBase } from '../core/testes';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService, textoDoErro } from '../core/mensagem.service';
import { Perfil } from '../core/modelos';
import { By } from '@angular/platform-browser';
import { OrcamentoItensComponent } from './orcamento-itens.component';
import { OrdemDetalheComponent } from './ordem-detalhe.component';
import { OrdemServico, StatusOS } from './ordens.modelos';
import { ordemServico, pagina } from './testes-ordens';


describe('OrdemDetalheComponent', () => {
  let fixture: ComponentFixture<OrdemDetalheComponent>;
  let componente: OrdemDetalheComponent;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let mensagens: jasmine.SpyObj<MensagemService>;

  /** Monta a tela com o perfil dado; `usuarioId` é o usuário logado (para RN09). */
  function montar(perfil: Perfil, os: OrdemServico, usuarioId = 2, confirma = true) {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogo.open.and.returnValue({ afterClosed: () => of(confirma) } as never);
    mensagens = mensagensFalsas();
    TestBed.configureTestingModule({
      imports: [OrdemDetalheComponent],
      providers: [
        provideRouter([]), ...provideTesteBase(),
        { provide: AuthService, useValue: authFalso(perfil, usuarioId) },
        { provide: MensagemService, useValue: mensagens },
      ],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(OrdemDetalheComponent);
    componente = fixture.componentInstance;
    api = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('id', String(os.id));
    fixture.detectChanges();
    if (perfil === 'ADMINISTRADOR') {
      api.expectOne((r) => r.url === '/api/usuarios').flush(pagina([
        { id: 5, nome: 'Raimundo', perfil: 'MECANICO', ativo: true },
        { id: 6, nome: 'Inativo', perfil: 'MECANICO', ativo: false },
        { id: 1, nome: 'Admin', perfil: 'ADMINISTRADOR', ativo: true },
      ]));
    }
    api.expectOne('/api/ordens/41').flush(os);
    fixture.detectChanges();
  }

  const itens = (): OrcamentoItensComponent =>
    fixture.debugElement.query(By.directive(OrcamentoItensComponent)).componentInstance;
  const el = (testid: string): HTMLElement | null => fixture.nativeElement.querySelector(`[data-testid="${testid}"]`);
  const texto = (testid: string): string => el(testid)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const clicar = (testid: string) => {
    el(testid)!.click();
    fixture.detectChanges();
  };
  const acoesVisiveis = (): string[] =>
    Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[data-testid^="acao-"]'))
      .map((b) => b.dataset['testid']!.replace('acao-', ''))
      .sort();

  afterEach(() => api.verify());

  describe('cabeçalho e histórico', () => {
    it('deve mostrar placa, veículo, cliente com telefone, km, problema e status', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));

      const cabecalho = texto('cabecalho');
      ['OS #1041', 'NBV8D45', 'Renault Sandero 2017 · prata', 'Rita Fictícia · (83) 99812-4410', '84.320 km',
        'Ruído ao frear e pedal baixo', 'Não atribuído'].forEach((t) => expect(cabecalho).toContain(t));
      expect(texto('status')).toBe('Aguardando aprovação');
    });

    it('deve listar o histórico de status do mais recente para o mais antigo', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO', {
        historico: [
          { statusAnterior: null, statusNovo: 'ABERTA', usuarioId: 2, usuario: 'Ana', dataHora: '2026-11-15T10:58:00' },
          { statusAnterior: 'ABERTA', statusNovo: 'AGUARDANDO_APROVACAO', usuarioId: 2, usuario: 'Ana', dataHora: '2026-11-15T11:20:00' },
        ],
      }));

      const itens = Array.from<HTMLElement>(el('historico')!.querySelectorAll('li')).map((li) => li.textContent!);
      expect(itens[0]).toContain('Aguardando aprovação · Ana');
      expect(itens[1]).toContain('Aberta · Ana');
    });
  });

  describe('resumo do orçamento (CT51)', () => {
    it('deve atualizar subtotais e total na hora ao remover e incluir itens', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));
      expect(texto('subtotal-servicos')).toContain('200,00');
      expect(texto('total')).toContain('370,50');

      (fixture.nativeElement.querySelectorAll('[data-testid="remover-servico"]')[0] as HTMLElement).click();
      fixture.detectChanges();
      expect(texto('subtotal-servicos')).toContain('80,00');
      expect(texto('total')).toContain('250,50');

      itens().buscaPeca.setValue({ id: 23, codigo: 'FLT-01', descricao: 'Filtro de óleo', precoVenda: 35, ativo: true });
      itens().quantidadePeca.setValue(2);
      clicar('adicionar-peca');
      expect(texto('subtotal-pecas')).toContain('260,00');
      expect(texto('total')).toContain('320,50');
      expect(texto('pecas')).toContain('Filtro de óleo');
    });

    it('deve recalcular ao mudar a quantidade de uma peça', () => {
      montar('ATENDENTE', ordemServico('ABERTA'));

      const quantidade = fixture.nativeElement.querySelectorAll('[data-testid="quantidade"]')[1] as HTMLInputElement;
      quantidade.value = '4';
      quantidade.dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(texto('subtotal-pecas')).toContain('235,00');
    });

    it('deve buscar serviços no catálogo enquanto o usuário digita', fakeAsync(() => {
      montar('ATENDENTE', ordemServico('ABERTA'));

      itens().buscaServico.setValue('alin');
      tick(250);

      const req = api.expectOne((r) => r.url === '/api/servicos');
      expect(req.request.params.get('busca')).toBe('alin');
      req.flush(pagina([{ id: 13, descricao: 'Alinhamento', precoMaoDeObra: 60, ativo: true }]));
      expect(itens().servicosEncontrados().map((s) => s.descricao)).toEqual(['Alinhamento']);
    }));

    it('deve salvar o orçamento com itens existentes por itemId, novos pelo catálogo e a versão da OS', () => {
      montar('ATENDENTE', ordemServico('ABERTA'));
      itens().buscaServico.setValue({ id: 13, descricao: 'Alinhamento', precoMaoDeObra: 60, ativo: true });
      clicar('adicionar-servico');
      expect(el('acao-compartilhar')).toBeNull();

      clicar('salvar-orcamento');

      const req = api.expectOne('/api/ordens/41/orcamentos/1');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.servicos).toEqual([
        { itemId: 1, servicoId: null }, { itemId: 2, servicoId: null }, { itemId: null, servicoId: 13 },
      ]);
      expect(req.request.body.versaoOs).toBe(3);
      req.flush(ordemServico('ABERTA', { version: 4 }, { total: 430.5 }));
      fixture.detectChanges();
      expect(mensagens.sucesso).toHaveBeenCalledWith('Orçamento salvo.');
      expect(el('acao-compartilhar')).not.toBeNull();
    });

    it('deve avisar o atendente quando o desconto passar de 10% (RN05)', () => {
      montar('ATENDENTE', ordemServico('ABERTA'));
      expect(el('aviso-desconto')).toBeNull();

      componente.desconto.setValue(40);
      fixture.detectChanges();

      expect(texto('aviso-desconto')).toContain('limite de 10%');
    });

    it('não deve avisar limite de desconto para o administrador', () => {
      montar('ADMINISTRADOR', ordemServico('ABERTA'));

      componente.desconto.setValue(100);
      fixture.detectChanges();

      expect(el('aviso-desconto')).toBeNull();
    });
  });

  describe('RN07: alterar orçamento aprovado', () => {
    it('deve avisar que será criada nova versão e enviar a alteração para a versão vigente', () => {
      montar('ATENDENTE', ordemServico('APROVADA'));
      expect(el('busca-servico')).toBeNull();
      expect(el('remover-servico')).toBeNull();

      clicar('alterar-aprovado');

      const dados = dialogo.open.calls.mostRecent().args[1]!.data as { mensagem: string };
      expect(dados.mensagem).toContain('será criada a versão 2');
      expect(dados.mensagem).toContain('Aguardando aprovação');
      expect(texto('aviso-nova-versao')).toContain('versão 2');

      (fixture.nativeElement.querySelector('[data-testid="remover-peca"]') as HTMLElement).click();
      fixture.detectChanges();
      expect(texto('salvar-orcamento')).toBe('Salvar como versão 2');
      clicar('salvar-orcamento');

      api.expectOne((r) => r.method === 'PUT' && r.url === '/api/ordens/41/orcamentos/1')
        .flush(ordemServico('AGUARDANDO_APROVACAO', { version: 4 }, { versao: 2, situacao: 'PENDENTE' }));
      fixture.detectChanges();
      expect(mensagens.sucesso).toHaveBeenCalledWith(jasmine.stringContaining('Nova versão'));
      expect(texto('status')).toBe('Aguardando aprovação');
    });

    it('não deve liberar a edição se o usuário desistir no aviso', () => {
      montar('ATENDENTE', ordemServico('APROVADA'), 2, false);

      clicar('alterar-aprovado');

      expect(el('aviso-nova-versao')).toBeNull();
      expect(el('busca-servico')).toBeNull();
    });
  });

  describe('botões por status e perfil', () => {
    const casos: [StatusOS, Perfil, number | null, string[]][] = [
      ['ABERTA', 'ATENDENTE', null, ['cancelar', 'compartilhar', 'enviarParaAprovacao']],
      ['AGUARDANDO_APROVACAO', 'ATENDENTE', null, ['aprovar', 'cancelar', 'compartilhar', 'recusar']],
      ['APROVADA', 'ATENDENTE', 5, ['cancelar', 'compartilhar']],
      ['APROVADA', 'MECANICO', 5, ['iniciar']],
      ['APROVADA', 'MECANICO', 9, []],
      ['EM_EXECUCAO', 'MECANICO', 5, ['aguardarPeca', 'concluir']],
      ['AGUARDANDO_PECA', 'MECANICO', 5, ['iniciar']],
      ['CONCLUIDA', 'ATENDENTE', 5, ['cancelar', 'compartilhar', 'entregar']],
      ['ENTREGUE', 'ATENDENTE', 5, []],
      ['EM_EXECUCAO', 'ADMINISTRADOR', 5, ['aguardarPeca', 'atribuirMecanico', 'cancelar', 'compartilhar', 'concluir']],
    ];

    casos.forEach(([status, perfil, mecanicoId, esperado]) => {
      it(`${status} + ${perfil}${mecanicoId === 9 ? ' (OS de outro mecânico)' : ''}: ${esperado.join(', ') || 'nenhuma ação'}`, () => {
        const mecanico = mecanicoId === null ? null : { id: mecanicoId, nome: 'Raimundo' };
        montar(perfil, ordemServico(status, { mecanico }), perfil === 'MECANICO' ? 5 : 2);

        expect(acoesVisiveis()).toEqual(esperado);
      });
    });

    it('deve mostrar "Retomar serviço" quando a OS aguarda peça', () => {
      montar('MECANICO', ordemServico('AGUARDANDO_PECA', { mecanico: { id: 5, nome: 'Raimundo' } }), 5);
      expect(texto('acao-iniciar')).toBe('Retomar serviço');
    });

    it('não deve deixar o mecânico editar o orçamento', () => {
      montar('MECANICO', ordemServico('EM_EXECUCAO', { mecanico: { id: 5, nome: 'Raimundo' } }), 5);
      expect(el('busca-servico')).toBeNull();
      expect(el('alterar-aprovado')).toBeNull();
      expect(el('desconto')).toBeNull();
    });

    it('deve oferecer ao admin só mecânicos ativos e atribuir com a versão da OS', () => {
      montar('ADMINISTRADOR', ordemServico('APROVADA'));
      expect(componente.mecanicos()).toEqual([{ id: 5, nome: 'Raimundo' }]);

      componente.mecanicoEscolhido.setValue(5);
      fixture.detectChanges();
      clicar('acao-atribuirMecanico');

      const req = api.expectOne('/api/ordens/41/mecanico');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ mecanicoId: 5, versaoOs: 3 });
      req.flush(ordemServico('APROVADA', { mecanico: { id: 5, nome: 'Raimundo' } }));
      fixture.detectChanges();
      expect(texto('mecanico')).toBe('Raimundo');
    });
  });

  describe('ações', () => {
    it('deve mudar o status pela API e mostrar o novo status', () => {
      montar('MECANICO', ordemServico('APROVADA', { mecanico: { id: 5, nome: 'Raimundo' } }), 5);

      clicar('acao-iniciar');

      const req = api.expectOne('/api/ordens/41/status');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'EM_EXECUCAO', versaoOs: 3 });
      req.flush(ordemServico('EM_EXECUCAO', { mecanico: { id: 5, nome: 'Raimundo' } }));
      fixture.detectChanges();
      expect(texto('status')).toBe('Em execução');
      expect(acoesVisiveis()).toEqual(['aguardarPeca', 'concluir']);
    });

    it('deve registrar a aprovação da versão vigente', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));

      clicar('acao-aprovar');

      const req = api.expectOne('/api/ordens/41/orcamentos/1/aprovacao');
      expect(req.request.method).toBe('POST');
      req.flush(ordemServico('APROVADA'));
      fixture.detectChanges();
      expect(texto('status')).toBe('Aprovada');
    });

    it('deve mostrar a mensagem 422 da API sem mudar a tela (a API decide a regra)', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));

      clicar('acao-aprovar');
      api.expectOne('/api/ordens/41/orcamentos/1/aprovacao').flush(
        { status: 422, erro: 'Regra de negócio', mensagem: 'Orçamento já está APROVADO', campos: {} },
        { status: 422, statusText: 'Unprocessable Entity' },
      );
      fixture.detectChanges();

      const erro = mensagens.erro.calls.mostRecent().args[0] as HttpErrorResponse;
      expect(textoDoErro(erro)).toBe('Orçamento já está APROVADO');
      expect(texto('status')).toBe('Aguardando aprovação');
    });

    it('deve pedir confirmação antes de cancelar a OS', () => {
      montar('ATENDENTE', ordemServico('ABERTA'));

      clicar('acao-cancelar');

      expect(dialogo.open).toHaveBeenCalled();
      const req = api.expectOne('/api/ordens/41/status');
      expect(req.request.body).toEqual({ status: 'CANCELADA', versaoOs: 3 });
      req.flush(ordemServico('CANCELADA'));
      fixture.detectChanges();
      expect(acoesVisiveis()).toEqual([]);
    });

    it('não deve cancelar se o usuário voltar na confirmação', () => {
      montar('ATENDENTE', ordemServico('ABERTA'), 2, false);

      clicar('acao-cancelar');

      expect(dialogo.open).toHaveBeenCalled();
      api.expectNone('/api/ordens/41/status');
    });

    it('deve pedir confirmação antes de registrar a recusa', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));

      clicar('acao-recusar');

      expect(dialogo.open).toHaveBeenCalled();
      api.expectOne('/api/ordens/41/orcamentos/1/recusa').flush(ordemServico('RECUSADA'));
    });
  });

  describe('compartilhar (CT46)', () => {
    it('deve baixar o PDF do orçamento e abrir o link do WhatsApp com telefone e total', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));
      const salvar = spyOn(componente, 'salvarArquivo');
      const abrir = spyOn(componente, 'abrirLink').and.returnValue(true);
      const link = 'https://wa.me/5583998124410?text=Total%3A%20R%24%20370%2C50';

      clicar('acao-compartilhar');

      const pdf = api.expectOne('/api/ordens/41/orcamentos/1/pdf');
      expect(pdf.request.responseType).toBe('blob');
      pdf.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
      api.expectOne('/api/ordens/41/compartilhamento').flush({ linkWhatsapp: link });

      expect(salvar).toHaveBeenCalledWith(jasmine.any(Blob), 'orcamento-os-1041-v1.pdf');
      expect(abrir).toHaveBeenCalledWith(link);
      expect(mensagens.aviso).not.toHaveBeenCalled();
    });

    it('deve avisar quando o navegador bloquear a janela do WhatsApp', () => {
      montar('ATENDENTE', ordemServico('AGUARDANDO_APROVACAO'));
      spyOn(componente, 'salvarArquivo');
      spyOn(componente, 'abrirLink').and.returnValue(false);

      clicar('acao-compartilhar');
      api.expectOne('/api/ordens/41/orcamentos/1/pdf').flush(new Blob(['%PDF']));
      api.expectOne('/api/ordens/41/compartilhamento').flush({ linkWhatsapp: 'https://wa.me/5583998124410' });

      expect(mensagens.aviso).toHaveBeenCalledWith(jasmine.stringContaining('permita pop-ups'));
    });
  });
});
