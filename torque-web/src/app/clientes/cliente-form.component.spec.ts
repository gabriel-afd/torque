import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { MensagemService } from '../core/mensagem.service';
import { ClienteFormComponent } from './cliente-form.component';

describe('ClienteFormComponent', () => {
  let fixture: ComponentFixture<ClienteFormComponent>;
  let api: HttpTestingController;
  let mensagens: jasmine.SpyObj<MensagemService>;

  beforeEach(() => {
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['sucesso', 'erro', 'aviso']);
    TestBed.configureTestingModule({
      imports: [ClienteFormComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: MensagemService, useValue: mensagens },
      ],
    });
    fixture = TestBed.createComponent(ClienteFormComponent);
    api = TestBed.inject(HttpTestingController);
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();
  });

  afterEach(() => api.verify());

  function digitar(seletor: string, valor: string) {
    const campo: HTMLInputElement = fixture.nativeElement.querySelector(seletor);
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    campo.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  }

  function preencherValido() {
    fixture.componentInstance.form.setValue({
      nome: 'Joana Fictícia', documento: '529.982.247-25', telefone: '(83) 99999-0001', email: '',
    });
  }

  function enviar() {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('deve mostrar erro de dígito verificador no próprio formulário, sem chamar a API', () => {
    preencherValido();
    digitar('[data-testid="documento"]', '11111111112');
    enviar();

    expect(fixture.componentInstance.form.controls.documento.hasError('cpfCnpj')).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-testid="erro-documento"]').textContent).toContain('CPF/CNPJ inválido');
    api.expectNone('/api/clientes');
  });

  it('deve aplicar a máscara de CPF enquanto digita', () => {
    digitar('[data-testid="documento"]', '52998224725');

    expect(fixture.componentInstance.form.controls.documento.value).toBe('529.982.247-25');
  });

  it('deve cadastrar e ir para o detalhe do cliente', () => {
    preencherValido();
    enviar();

    const req = api.expectOne('/api/clientes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nome: 'Joana Fictícia', documento: '529.982.247-25', telefone: '(83) 99999-0001', email: null,
    });
    req.flush({ id: 7 });

    expect(mensagens.sucesso).toHaveBeenCalled();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/clientes', 7]);
  });

  it('deve mostrar o 409 de CPF duplicado no campo documento', () => {
    preencherValido();
    enviar();

    api.expectOne('/api/clientes').flush(
      { status: 409, erro: 'Conflict', mensagem: 'Já existe um cliente com este CPF/CNPJ', campos: {} },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.documento.getError('api')).toBe('Já existe um cliente com este CPF/CNPJ');
    expect(fixture.nativeElement.querySelector('[data-testid="erro-documento"]').textContent).toContain('Já existe');
    expect(mensagens.erro).not.toHaveBeenCalled();
  });

  it('deve mostrar os erros 400 da API nos campos indicados', () => {
    preencherValido();
    enviar();

    api.expectOne('/api/clientes').flush(
      { status: 400, erro: 'Bad Request', mensagem: 'Dados inválidos', campos: { telefone: 'Telefone deve ter DDD' } },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(fixture.componentInstance.form.controls.telefone.getError('api')).toBe('Telefone deve ter DDD');
    expect(mensagens.erro).not.toHaveBeenCalled();
  });

  it('deve mostrar mensagem geral quando o erro não for de campo', () => {
    preencherValido();
    enviar();

    api.expectOne('/api/clientes').flush({}, { status: 500, statusText: 'Server Error' });

    expect(mensagens.erro).toHaveBeenCalled();
  });
});
