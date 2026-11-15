import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { Perfil } from '../core/modelos';
import { BuscaPlacaComponent } from './busca-placa.component';
import { CadastroRapidoComponent } from './cadastro-rapido.component';
import { VeiculoComDono } from './clientes.modelos';

const VEICULO: VeiculoComDono = {
  id: 3, placa: 'ABC1D23', marca: 'Fiat', modelo: 'Uno', ano: 2015, cor: 'Branco',
  dono: { id: 9, nome: 'Joana Fictícia', documento: '52998224725', telefone: '83999990001', email: null, ativo: true },
};

describe('BuscaPlacaComponent', () => {
  let fixture: ComponentFixture<BuscaPlacaComponent>;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let encontrados: VeiculoComDono[];

  function montar(perfil: Perfil) {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      imports: [BuscaPlacaComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: AuthService, useValue: { temPerfil: (...p: Perfil[]) => p.includes(perfil) } },
      ],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(BuscaPlacaComponent);
    api = TestBed.inject(HttpTestingController);
    encontrados = [];
    fixture.componentInstance.encontrado.subscribe((v) => encontrados.push(v));
    fixture.detectChanges();
  }

  function buscar(placa: string) {
    const campo: HTMLInputElement = fixture.nativeElement.querySelector('[data-testid="busca-placa"]');
    campo.value = placa;
    campo.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const $ = (testid: string) => fixture.nativeElement.querySelector(`[data-testid="${testid}"]`);

  afterEach(() => api.verify());

  it('deve buscar a placa normalizada e emitir o veículo encontrado', () => {
    montar('ATENDENTE');
    buscar('abc-1d23');

    const req = api.expectOne((r) => r.url === '/api/veiculos');
    expect(req.request.params.get('placa')).toBe('ABC1D23');
    req.flush(VEICULO);
    fixture.detectChanges();

    expect(encontrados).toEqual([VEICULO]);
    expect($('veiculo-encontrado').textContent).toContain('Joana Fictícia');
    expect($('placa-nao-encontrada')).toBeNull();
  });

  it('placa não encontrada → deve oferecer o cadastro para o atendente', () => {
    montar('ATENDENTE');
    buscar('XYZ9A87');

    api.expectOne((r) => r.url === '/api/veiculos').flush(
      { status: 404, erro: 'Not Found', mensagem: 'Veículo não encontrado: XYZ9A87', campos: {} },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    expect($('placa-nao-encontrada').textContent).toContain('Veículo XYZ9A87 não encontrado');
    expect($('oferecer-cadastro')).not.toBeNull();
    expect(encontrados).toEqual([]);
  });

  it('ao cadastrar pela oferta, deve abrir o cadastro rápido com a placa e emitir o veículo criado', () => {
    montar('ATENDENTE');
    dialogo.open.and.returnValue({ afterClosed: () => of(VEICULO) } as never);
    buscar('ABC1D23');
    api.expectOne((r) => r.url === '/api/veiculos').flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    $('oferecer-cadastro').click();
    fixture.detectChanges();

    expect(dialogo.open).toHaveBeenCalledWith(CadastroRapidoComponent, jasmine.objectContaining({ data: { placa: 'ABC1D23' } }));
    expect(encontrados).toEqual([VEICULO]);
    expect($('placa-nao-encontrada')).toBeNull();
  });

  it('placa não encontrada → não deve oferecer cadastro ao mecânico', () => {
    montar('MECANICO');
    buscar('XYZ9A87');

    api.expectOne((r) => r.url === '/api/veiculos').flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect($('placa-nao-encontrada').textContent).toContain('Peça ao balcão');
    expect($('oferecer-cadastro')).toBeNull();
  });

  it('não deve chamar a API com placa inválida', () => {
    montar('ATENDENTE');
    buscar('ABCD123');

    api.expectNone((r) => r.url === '/api/veiculos');
    expect(fixture.componentInstance.placa.hasError('placa')).toBeTrue();
  });
});
