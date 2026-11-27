import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MensagemService } from '../core/mensagem.service';
import { CadastroRapidoComponent } from './cadastro-rapido.component';

const CLIENTE = { id: 9, nome: 'Joana Fictícia', documento: '52998224725', telefone: '83999990001', email: null, ativo: true };
const VEICULO = { id: 3, clienteId: 9, placa: 'XYZ9A87', marca: 'Fiat', modelo: 'Uno', ano: 2015, cor: null };

describe('CadastroRapidoComponent', () => {
  let fixture: ComponentFixture<CadastroRapidoComponent>;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialogRef<CadastroRapidoComponent>>;
  let mensagens: jasmine.SpyObj<MensagemService>;

  beforeEach(() => {
    dialogo = jasmine.createSpyObj('MatDialogRef', ['close']);
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['sucesso', 'erro', 'aviso']);
    TestBed.configureTestingModule({
      imports: [CadastroRapidoComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogo },
        { provide: MAT_DIALOG_DATA, useValue: { placa: 'XYZ9A87' } },
        { provide: MensagemService, useValue: mensagens },
      ],
    });
    fixture = TestBed.createComponent(CadastroRapidoComponent);
    api = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => api.verify());

  function preencherVeiculo() {
    fixture.componentInstance.formVeiculo.patchValue({ marca: 'Fiat', modelo: 'Uno', ano: 2015 });
  }

  it('deve vir com a placa buscada já preenchida', () => {
    expect(fixture.componentInstance.formVeiculo.controls.placa.value).toBe('XYZ9A87');
    expect(fixture.nativeElement.querySelector('[mat-dialog-title]').textContent).toContain('Cadastrar veículo XYZ9A87');
  });

  it('cliente novo: deve cadastrar o cliente, depois o veículo, e devolver o veículo com o dono', () => {
    const c = fixture.componentInstance;
    c.modo.set('novo');
    c.formCliente.setValue({ nome: 'Joana Fictícia', documento: '529.982.247-25', telefone: '(83) 99999-0001', email: '' });
    preencherVeiculo();

    c.salvar();
    api.expectOne((r) => r.method === 'POST' && r.url === '/api/clientes').flush(CLIENTE);
    const reqVeiculo = api.expectOne('/api/clientes/9/veiculos');
    expect(reqVeiculo.request.body.placa).toBe('XYZ9A87');
    reqVeiculo.flush(VEICULO);

    expect(dialogo.close).toHaveBeenCalledWith(jasmine.objectContaining({
      placa: 'XYZ9A87', dono: jasmine.objectContaining({ id: 9, nome: 'Joana Fictícia' }),
    }));
  });

  it('cliente existente: deve buscar pelo nome/CPF e cadastrar o veículo para o cliente escolhido', fakeAsync(() => {
    const c = fixture.componentInstance;
    c.buscaCliente.setValue('Joana');
    tick(300);
    api.expectOne((r) => r.url === '/api/clientes' && r.params.get('busca') === 'Joana')
      .flush({ content: [CLIENTE], page: { size: 5, number: 0, totalElements: 1, totalPages: 1 } });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="opcao-cliente"]').click();
    preencherVeiculo();
    c.salvar();

    api.expectOne('/api/clientes/9/veiculos').flush(VEICULO);
    expect(dialogo.close).toHaveBeenCalled();
  }));

  it('deve avisar quando nenhum cliente foi escolhido', () => {
    preencherVeiculo();

    fixture.componentInstance.salvar();

    expect(mensagens.aviso).toHaveBeenCalled();
    api.expectNone('/api/clientes/9/veiculos');
  });

  it('placa duplicada (409) deve aparecer no campo placa sem recriar o cliente novo', () => {
    const c = fixture.componentInstance;
    c.modo.set('novo');
    c.formCliente.setValue({ nome: 'Joana Fictícia', documento: '529.982.247-25', telefone: '(83) 99999-0001', email: '' });
    preencherVeiculo();

    c.salvar();
    api.expectOne((r) => r.method === 'POST' && r.url === '/api/clientes').flush(CLIENTE);
    api.expectOne('/api/clientes/9/veiculos').flush(
      { status: 409, erro: 'Conflict', mensagem: 'Já existe um veículo com esta placa', campos: {} },
      { status: 409, statusText: 'Conflict' },
    );

    expect(c.formVeiculo.controls.placa.getError('api')).toBe('Já existe um veículo com esta placa');
    expect(c.modo()).toBe('existente');
    expect(c.selecionado()?.id).toBe(9);
    expect(dialogo.close).not.toHaveBeenCalled();
  });
});
