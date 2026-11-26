import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { mensagensFalsas, provideTesteBase } from '../core/testes';
import { ClienteListaComponent } from './cliente-lista.component';
import { NovoClienteComponent } from './novo-cliente.component';

describe('NovoClienteComponent (diálogo)', () => {
  let fixture: ComponentFixture<NovoClienteComponent>;
  let api: HttpTestingController;
  let mensagens: jasmine.SpyObj<MensagemService>;
  let dialogo: jasmine.SpyObj<MatDialogRef<NovoClienteComponent>>;

  beforeEach(() => {
    mensagens = mensagensFalsas();
    dialogo = jasmine.createSpyObj<MatDialogRef<NovoClienteComponent>>('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [NovoClienteComponent],
      providers: [
        ...provideTesteBase(),
        { provide: MensagemService, useValue: mensagens },
        { provide: MatDialogRef, useValue: dialogo },
      ],
    });
    fixture = TestBed.createComponent(NovoClienteComponent);
    api = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => api.verify());

  function preencherEEnviar() {
    fixture.componentInstance.form.setValue({
      nome: 'Joana Fictícia', documento: '529.982.247-25', telefone: '(83) 99999-0001', email: '',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('deve cadastrar e fechar devolvendo o cliente', () => {
    preencherEEnviar();

    const req = api.expectOne('/api/clientes');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 7, nome: 'Joana Fictícia' });

    expect(mensagens.sucesso).toHaveBeenCalledWith('Cliente cadastrado.');
    expect(dialogo.close).toHaveBeenCalledWith(jasmine.objectContaining({ id: 7 }));
  });

  it('não deve chamar a API com o formulário inválido', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    api.expectNone('/api/clientes');
    expect(fixture.componentInstance.form.controls.nome.touched).toBeTrue();
  });

  it('deve mostrar o 409 de CPF duplicado no campo e manter o diálogo aberto', () => {
    preencherEEnviar();
    api.expectOne('/api/clientes').flush(
      { status: 409, erro: 'Conflict', mensagem: 'Já existe um cliente com este CPF/CNPJ', campos: {} },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="erro-documento"]').textContent).toContain('Já existe');
    expect(dialogo.close).not.toHaveBeenCalled();
  });
});

describe('ClienteListaComponent (novo cliente em diálogo)', () => {
  let fixture: ComponentFixture<ClienteListaComponent>;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let router: Router;

  function montar(cadastrado: object | undefined, novoPelaRota = false) {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogo.open.and.returnValue({ afterClosed: () => of(cadastrado) } as never);
    TestBed.configureTestingModule({
      imports: [ClienteListaComponent],
      providers: [provideRouter([]), ...provideTesteBase(), { provide: MensagemService, useValue: mensagensFalsas() }],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(ClienteListaComponent);
    fixture.componentRef.setInput('novo', novoPelaRota);
    api = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture.detectChanges();
    api.expectOne((r) => r.url === '/api/clientes').flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
  }

  afterEach(() => api.verify());

  it('"Novo cliente" deve abrir o cadastro em diálogo e ir para o cliente cadastrado', () => {
    montar({ id: 7 });

    fixture.nativeElement.querySelector('[data-testid="novo-cliente"]').click();

    expect(dialogo.open).toHaveBeenCalledWith(NovoClienteComponent, jasmine.any(Object));
    expect(router.navigate).toHaveBeenCalledWith(['/clientes', 7]);
  });

  it('não deve sair da lista quando o diálogo for cancelado', () => {
    montar(undefined);

    fixture.nativeElement.querySelector('[data-testid="novo-cliente"]').click();

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('/clientes/novo: deve abrir o diálogo sobre a lista e voltar para /clientes ao cancelar', () => {
    montar(undefined, true);

    expect(dialogo.open).toHaveBeenCalledWith(NovoClienteComponent, jasmine.any(Object));
    expect(router.navigate).toHaveBeenCalledWith(['/clientes'], { replaceUrl: true });
  });
});
