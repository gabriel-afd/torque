import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { Perfil } from '../core/modelos';
import { NovaOsComponent } from './nova-os.component';
import { QuadroComponent } from './quadro.component';
import { ordem, pagina } from './testes-ordens';

describe('QuadroComponent', () => {
  let fixture: ComponentFixture<QuadroComponent>;
  let api: HttpTestingController;
  let router: Router;
  let dialogo: jasmine.SpyObj<MatDialog>;

  function montar(perfil: Perfil) {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      imports: [QuadroComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: AuthService, useValue: { perfil: () => perfil, temPerfil: (...p: Perfil[]) => p.includes(perfil) } },
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
    expect(fixture.nativeElement.querySelector('[data-testid="total-andamento"]').textContent).toContain('4 OS');
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
});
