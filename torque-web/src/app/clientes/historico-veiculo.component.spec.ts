import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { Perfil } from '../core/modelos';
import { NovaOsComponent } from '../ordens/nova-os.component';
import { pagina } from '../ordens/testes-ordens';
import { OsDoVeiculo, VeiculoComDono } from './clientes.modelos';
import { HistoricoVeiculoComponent } from './historico-veiculo.component';

registerLocaleData(localePt);

const veiculo: VeiculoComDono = {
  id: 9, placa: 'MZK2310', marca: 'VW', modelo: 'Gol 1.0', ano: 2014, cor: 'branco',
  dono: { id: 4, nome: 'Pedro Fictício', telefone: '83988771203', ativo: true },
};

function os(numero: number, extra: Partial<OsDoVeiculo>): OsDoVeiculo {
  return {
    id: numero, numero, status: 'ENTREGUE', dataAbertura: '2026-05-12T09:00:00', dataEntrega: null, quilometragem: null,
    problemaRelatado: 'Revisão', servicos: [], total: null, ...extra,
  };
}

describe('HistoricoVeiculoComponent', () => {
  let fixture: ComponentFixture<HistoricoVeiculoComponent>;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let mensagens: jasmine.SpyObj<MensagemService>;

  function montar(perfil: Perfil, placa = 'mzk2310') {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['sucesso', 'aviso', 'erro']);
    TestBed.configureTestingModule({
      imports: [HistoricoVeiculoComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: AuthService, useValue: { perfil: () => perfil, temPerfil: (...p: Perfil[]) => p.includes(perfil) } },
        { provide: MensagemService, useValue: mensagens },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
      ],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(HistoricoVeiculoComponent);
    api = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('placa', placa);
    fixture.detectChanges();
  }

  function responder(ordens: OsDoVeiculo[], total = ordens.length) {
    api.expectOne((r) => r.url === '/api/veiculos' && r.params.get('placa') === 'MZK2310').flush(veiculo);
    api.expectOne((r) => r.url === '/api/veiculos/9/ordens').flush({ ...pagina(ordens), page: { size: 50, number: 0, totalElements: total, totalPages: 1 } });
    fixture.detectChanges();
  }

  const texto = (seletor: string): string => fixture.nativeElement.querySelector(seletor)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const cartoes = (): string[] =>
    Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[data-testid="os"]')).map((c) => c.textContent!.replace(/\s+/g, ' '));

  afterEach(() => api.verify());

  it('deve mostrar veículo, dono, total de OS e última quilometragem', () => {
    montar('ATENDENTE');
    responder([
      os(1043, { status: 'ABERTA', quilometragem: null, dataAbertura: '2026-11-16T08:00:00' }),
      os(911, { quilometragem: 52410 }),
      os(802, { quilometragem: 47150 }),
    ], 4);

    const cartao = texto('[data-testid="veiculo"]');
    ['MZK2310', 'VW Gol 1.0 · 2014 · branco', 'Pedro Fictício', '(83) 98877-1203', 'Ver cliente'].forEach((t) => expect(cartao).toContain(t));
    expect(texto('[data-testid="total-os"]')).toBe('4');
    expect(texto('[data-testid="ultima-km"]')).toBe('52.410 km');
  });

  it('deve listar as OS na ordem da API (mais recente primeiro) com data, km, serviços e valor', () => {
    montar('ATENDENTE');
    responder([
      os(911, { dataAbertura: '2026-05-12T09:00:00', quilometragem: 52410, problemaRelatado: 'Troca de óleo e filtros',
        servicos: ['Troca de óleo', 'Filtro de ar'], total: 310 }),
      os(745, { status: 'RECUSADA', dataAbertura: '2025-08-02T09:00:00', problemaRelatado: 'Amortecedores traseiros', total: 980 }),
    ]);

    const [primeiro, segundo] = cartoes();
    ['OS #911 · 12/05/2026 · 52.410 km', 'Entregue', 'Troca de óleo e filtros', 'Troca de óleo, Filtro de ar', 'R$ 310,00']
      .forEach((t) => expect(primeiro).toContain(t));
    expect(segundo).toContain('OS #745 · 02/08/2025');
    expect(segundo).toContain('Orçamento de R$ 980,00 recusado pelo cliente');
  });

  it('deve mostrar "Aguardando orçamento" para OS aberta sem serviços', () => {
    montar('ATENDENTE');
    responder([os(1043, { status: 'ABERTA' })]);
    expect(cartoes()[0]).toContain('Aguardando orçamento');
  });

  it('deve avisar quando a placa não estiver cadastrada', () => {
    montar('ATENDENTE');
    api.expectOne((r) => r.url === '/api/veiculos').flush(
      { status: 404, erro: 'Não encontrado', mensagem: 'Veículo não encontrado', campos: {} },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    expect(texto('[data-testid="nao-encontrado"]')).toContain('não encontrado');
    expect(mensagens.erro).not.toHaveBeenCalled();
  });

  it('deve abrir a nova OS já com o veículo pelo botão flutuante e ir para a OS criada', () => {
    montar('ATENDENTE');
    responder([]);
    dialogo.open.and.returnValue({ afterClosed: () => of({ id: 77, numero: 1077 }) } as never);
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    (fixture.nativeElement.querySelector('[data-testid="fab-nova-os"]') as HTMLElement).click();

    expect(dialogo.open).toHaveBeenCalledWith(NovaOsComponent, jasmine.objectContaining({ data: { veiculo } }));
    expect(router.navigate).toHaveBeenCalledWith(['/ordens', 77]);
  });

  it('não deve mostrar "+ Nova OS" nem "Ver cliente" para o mecânico', () => {
    montar('MECANICO');
    responder([]);

    expect(fixture.nativeElement.querySelector('[data-testid="fab-nova-os"]')).toBeNull();
    expect(texto('[data-testid="veiculo"]')).not.toContain('Ver cliente');
  });
});
