import { authFalso, mensagensFalsas, provideTesteBase } from '../core/testes';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { Perfil } from '../core/modelos';
import { NovaOsComponent } from './nova-os.component';
import { QuadroComponent } from './quadro.component';
import { OrdemResumo, StatusOS } from './ordens.modelos';
import { ordem, ordemServico, pagina } from './testes-ordens';

describe('QuadroComponent', () => {
  let fixture: ComponentFixture<QuadroComponent>;
  let api: HttpTestingController;
  let router: Router;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let mensagens: jasmine.SpyObj<MensagemService>;

  function montar(perfil: Perfil, usuarioId = 2) {
    mensagens = mensagensFalsas();
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      imports: [QuadroComponent],
      providers: [
        provideRouter([]), ...provideTesteBase(),
        { provide: AuthService, useValue: authFalso(perfil, usuarioId) },
        { provide: MensagemService, useValue: mensagens },
      ],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(QuadroComponent);
    api = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture.detectChanges();
  }

  const requisicaoDoQuadro = (): TestRequest => api.expectOne((r) => r.url === '/api/ordens');
  const todos = (seletor: string): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll(seletor));
  const coluna = (status: string): HTMLElement => fixture.nativeElement.querySelector(`[data-status="${status}"]`);

  afterEach(() => api.verify());

  it('deve pedir à API só as OS dos seis status do quadro', () => {
    montar('ATENDENTE');

    const req = requisicaoDoQuadro();
    expect(req.request.params.getAll('status')).toEqual([
      'ABERTA', 'AGUARDANDO_APROVACAO', 'APROVADA', 'EM_EXECUCAO', 'AGUARDANDO_PECA', 'CONCLUIDA',
    ]);
    expect(req.request.params.has('mecanico')).toBeFalse();
    req.flush(pagina([]));
  });

  it('deve mostrar o texto da busca inteiro sem precisar clicar (placeholder, não rótulo flutuante)', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([]));
    const campo: HTMLElement = fixture.nativeElement.querySelector('[data-testid="busca-quadro"]').closest('mat-form-field');

    expect(campo.querySelector('mat-label, .mdc-floating-label')).toBeNull();
    expect(campo.querySelector('input')!.getAttribute('placeholder')).toBe('Buscar placa ou cliente');
  });

  it('deve agrupar os cartões por status com contador em cada coluna', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([
      ordem('ABERTA', { placa: 'QFR4E21' }), ordem('ABERTA', { placa: 'MZK2310' }),
      ordem('AGUARDANDO_APROVACAO', { placa: 'OFG7H12' }), ordem('EM_EXECUCAO', { placa: 'LKJ9087' }),
    ]));
    fixture.detectChanges();

    expect(todos('[data-testid="contador"]').map((c) => c.textContent!.trim())).toEqual(['2', '1', '0', '1', '0', '0']);
    expect(coluna('ABERTA').textContent).toContain('QFR4E21');
    expect(coluna('ABERTA').textContent).toContain('MZK2310');
    expect(coluna('EM_EXECUCAO').textContent).toContain('LKJ9087');
    expect(coluna('APROVADA').textContent).toContain('Nenhuma OS');
    expect(fixture.nativeElement.querySelector('[data-testid="total-andamento"]').textContent).toContain('4 ordens de serviço');
  });

  it('deve mostrar o resumo do topo com os números do quadro', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([
      ordem('ABERTA'), ordem('AGUARDANDO_APROVACAO'), ordem('AGUARDANDO_APROVACAO'), ordem('EM_EXECUCAO'), ordem('CONCLUIDA'),
    ]));
    fixture.detectChanges();

    const numeros = todos('[data-testid="resumo-quadro"] b').map((b) => b.textContent!.trim());
    expect(numeros).toEqual(['05', '02', '01', '01']);
  });

  it('deve mostrar número da OS, placa, veículo, cliente e mecânico no cartão', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([
      ordem('APROVADA', { numero: 1033, placa: 'PJH3F90', veiculo: 'Hyundai HB20', cliente: 'Bruno Fictício', mecanicoId: 5, mecanico: 'Raimundo' }),
    ]));
    fixture.detectChanges();

    const cartao = todos('[data-testid="cartao"]')[0].textContent!;
    ['OS #1033', 'PJH3F90', 'Hyundai HB20', 'Bruno Fictício', 'Raimundo', 'R$ 190,00'].forEach((texto) =>
      expect(cartao).toContain(texto),
    );
  });

  it('deve destacar em vermelho o orçamento enviado há mais de 1 dia', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([ordem('AGUARDANDO_APROVACAO', { dataStatus: '2020-01-01T08:00:00' })]));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="situacao"]').classList).toContain('alerta');
  });

  it('deve filtrar pela busca de placa ou cliente', fakeAsync(() => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([]));

    fixture.componentInstance.busca.setValue('QFR');
    tick(300);

    const req = requisicaoDoQuadro();
    expect(req.request.params.get('busca')).toBe('QFR');
    req.flush(pagina([ordem('ABERTA', { placa: 'QFR4E21' })]));
  }));

  it('deve filtrar por mecânico e manter a lista de mecânicos no filtro', () => {
    montar('ADMINISTRADOR');
    requisicaoDoQuadro().flush(pagina([
      ordem('APROVADA', { mecanicoId: 5, mecanico: 'Raimundo' }), ordem('EM_EXECUCAO', { mecanicoId: 6, mecanico: 'Tiago' }),
    ]));

    fixture.componentInstance.mecanico.setValue(6);
    const req = requisicaoDoQuadro();
    expect(req.request.params.get('mecanico')).toBe('6');
    req.flush(pagina([ordem('EM_EXECUCAO', { mecanicoId: 6, mecanico: 'Tiago' })]));

    expect(fixture.componentInstance.mecanicos().map((m) => m.nome)).toEqual(['Raimundo', 'Tiago']);
  });

  it('MECANICO: não deve ver o filtro de mecânico nem o botão de nova OS', () => {
    montar('MECANICO');
    requisicaoDoQuadro().flush(pagina([ordem('EM_EXECUCAO', { mecanicoId: 5, mecanico: 'Mário' })]));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="filtro-mecanico"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="nova-os"]')).toBeNull();
  });

  it('deve abrir a tela da OS ao clicar no cartão', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([ordem('ABERTA', { id: 42 })]));
    fixture.detectChanges();

    todos('[data-testid="cartao"]')[0].click();

    expect(router.navigate).toHaveBeenCalledWith(['/ordens', 42]);
  });

  it('+ Nova OS: deve abrir o diálogo e ir para a OS criada', () => {
    montar('ATENDENTE');
    requisicaoDoQuadro().flush(pagina([]));
    dialogo.open.and.returnValue({ afterClosed: () => of({ id: 77, numero: 1077 }) } as never);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="nova-os"]').click();

    expect(dialogo.open).toHaveBeenCalledWith(NovaOsComponent, jasmine.any(Object));
    expect(router.navigate).toHaveBeenCalledWith(['/ordens', 77]);
  });

  // Arrastar cartões ---------------------------------------------------------------------------

  /** Simula o CDK soltando o cartão da coluna `de` na coluna `para`. */
  function soltar(os: OrdemResumo, para: StatusOS) {
    fixture.componentInstance.soltar({
      item: { data: os }, previousContainer: { data: os.status }, container: { data: para },
    } as unknown as CdkDragDrop<StatusOS, StatusOS, OrdemResumo>);
    fixture.detectChanges();
  }

  function comCartao(perfil: Perfil, os: OrdemResumo, usuarioId = 2) {
    montar(perfil, usuarioId);
    requisicaoDoQuadro().flush(pagina([os]));
    fixture.detectChanges();
  }

  it('arrastar de Aberta para Aguardando aprovação: deve enviar o orçamento com a versão atual da OS', () => {
    const os = ordem('ABERTA', { id: 41, numero: 1041 });
    comCartao('ATENDENTE', os);

    soltar(os, 'AGUARDANDO_APROVACAO');
    expect(coluna('AGUARDANDO_APROVACAO').textContent).toContain('OS #1041');

    api.expectOne('/api/ordens/41').flush(ordemServico('ABERTA', { version: 7 }));
    const patch = api.expectOne('/api/ordens/41/status');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ status: 'AGUARDANDO_APROVACAO', versaoOs: 7 });
    patch.flush(ordemServico('AGUARDANDO_APROVACAO'));

    expect(mensagens.sucesso).toHaveBeenCalledWith('OS #1041: Orçamento enviado para aprovação do cliente.');
    requisicaoDoQuadro().flush(pagina([{ ...os, status: 'AGUARDANDO_APROVACAO' }]));
  });

  it('arrastar de Aguardando aprovação para Aprovada: deve registrar a aprovação da versão vigente do orçamento', () => {
    const os = ordem('AGUARDANDO_APROVACAO', { id: 41 });
    comCartao('ATENDENTE', os);

    soltar(os, 'APROVADA');

    api.expectOne('/api/ordens/41').flush(ordemServico('AGUARDANDO_APROVACAO', {}, { versao: 2 }));
    const aprovacao = api.expectOne('/api/ordens/41/orcamentos/2/aprovacao');
    expect(aprovacao.request.method).toBe('POST');
    aprovacao.flush(ordemServico('APROVADA'));
    requisicaoDoQuadro().flush(pagina([]));

    expect(mensagens.sucesso).toHaveBeenCalled();
  });

  it('arrastar de Aberta direto para Concluída: deve explicar o motivo sem chamar a API', () => {
    const os = ordem('ABERTA', { numero: 1050 });
    comCartao('ADMINISTRADOR', os);

    soltar(os, 'CONCLUIDA');

    expect(mensagens.aviso).toHaveBeenCalledWith('Só OS em execução podem ser concluídas.');
    expect(coluna('ABERTA').textContent).toContain('OS #1050');
    expect(coluna('CONCLUIDA').textContent).not.toContain('OS #1050');
  });

  it('MECANICO arrastando a OS de outro mecânico: deve avisar (RN09) sem chamar a API', () => {
    const os = ordem('APROVADA', { mecanicoId: 8, mecanico: 'Tiago' });
    comCartao('MECANICO', os, 5);

    soltar(os, 'EM_EXECUCAO');

    expect(mensagens.aviso).toHaveBeenCalledWith('Só o mecânico atribuído a esta OS ou o administrador pode fazer essa mudança.');
  });

  it('MECANICO atribuído: deve iniciar a execução arrastando para Em execução', () => {
    const os = ordem('APROVADA', { id: 41, mecanicoId: 5, mecanico: 'Raimundo' });
    comCartao('MECANICO', os, 5);

    soltar(os, 'EM_EXECUCAO');

    api.expectOne('/api/ordens/41').flush(ordemServico('APROVADA'));
    expect(api.expectOne('/api/ordens/41/status').request.body.status).toBe('EM_EXECUCAO');
  });

  it('quando a API recusar o movimento: deve devolver o cartão e mostrar a mensagem da API', () => {
    const os = ordem('ABERTA', { id: 41, numero: 1041 });
    comCartao('ATENDENTE', os);

    soltar(os, 'AGUARDANDO_APROVACAO');
    api.expectOne('/api/ordens/41').flush(ordemServico('ABERTA'));
    api.expectOne('/api/ordens/41/status').flush(
      { status: 422, erro: 'Unprocessable Entity', mensagem: 'Inclua ao menos um item no orçamento', campos: {} },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(coluna('ABERTA').textContent).toContain('OS #1041');
    expect(coluna('AGUARDANDO_APROVACAO').textContent).not.toContain('OS #1041');
    expect(mensagens.erro).toHaveBeenCalledWith(jasmine.any(HttpErrorResponse));
    requisicaoDoQuadro().flush(pagina([os]));
  });

  it('soltar o cartão na própria coluna: não deve fazer nada', () => {
    const os = ordem('ABERTA');
    comCartao('ATENDENTE', os);

    soltar(os, 'ABERTA');

    expect(mensagens.aviso).not.toHaveBeenCalled();
  });

  it('não deve abrir a OS com o clique que o navegador dispara logo após soltar o cartão', () => {
    const os = ordem('ABERTA', { id: 42 });
    comCartao('ATENDENTE', os);

    fixture.componentInstance.fimDoArraste();
    todos('[data-testid="cartao"]')[0].click();

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
