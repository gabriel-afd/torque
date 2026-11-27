import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { CHAVE_SESSAO } from '../core/auth.service';
import { respostaLogin } from '../core/testes';
import { LoginComponent, MENSAGEM_CREDENCIAIS_INVALIDAS } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let api: HttpTestingController;
  let router: Router;

  function montar(retorno: string | null = null) {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    });
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { queryParamMap: convertToParamMap(retorno ? { retorno } : {}) } },
    });
    fixture = TestBed.createComponent(LoginComponent);
    api = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  function preencher(email: string, senha: string) {
    fixture.componentInstance.form.setValue({ email, senha });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  beforeEach(() => localStorage.removeItem(CHAVE_SESSAO));
  afterEach(() => {
    api.verify();
    localStorage.removeItem(CHAVE_SESSAO);
  });

  it('deve mostrar as chaves cruzadas da marca ao lado do nome', () => {
    montar();
    const icone: HTMLElement = fixture.nativeElement.querySelector('.logo .icone');

    expect(icone).not.toBeNull();
    expect(getComputedStyle(icone).maskImage).toContain('/icones/marca.svg');
  });

  it('deve mostrar o crédito do autor na tela inicial', () => {
    montar();

    expect(fixture.nativeElement.querySelector('[data-testid="credito"]').textContent.replace(/\s+/g, ' ').trim())
      .toBe('Desenvolvido por Gabriel Medeiros de Mendonça');
  });

  it('deve mostrar e ocultar a senha pelo botão do campo', () => {
    montar();
    const senha: HTMLInputElement = fixture.nativeElement.querySelector('[data-testid="senha"]');
    const botao: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testid="mostrar-senha"]');
    expect(senha.type).toBe('password');

    botao.click();
    fixture.detectChanges();
    expect(senha.type).toBe('text');
    expect(botao.getAttribute('aria-label')).toBe('Ocultar senha');

    botao.click();
    fixture.detectChanges();
    expect(senha.type).toBe('password');
  });

  it('deve mostrar "E-mail ou senha inválidos" quando a API recusar o login', () => {
    montar();
    preencher('ana@oficina.teste', 'errada');

    api.expectOne('/api/auth/login').flush(
      { status: 401, erro: 'Unauthorized', mensagem: 'E-mail ou senha inválidos', campos: {} },
      { status: 401, statusText: 'Unauthorized' },
    );
    fixture.detectChanges();

    const erro = fixture.nativeElement.querySelector('[data-testid="erro-login"]');
    expect(erro.textContent.trim()).toBe(MENSAGEM_CREDENCIAIS_INVALIDAS);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('deve entrar e ir para o quadro', () => {
    montar();
    preencher('ana@oficina.teste', 'senha-ficticia');

    api.expectOne('/api/auth/login').flush(respostaLogin('ATENDENTE'));

    expect(router.navigateByUrl).toHaveBeenCalledWith('/quadro');
  });

  it('deve voltar para a página pedida antes do login', () => {
    montar('/clientes');
    preencher('ana@oficina.teste', 'senha-ficticia');

    api.expectOne('/api/auth/login').flush(respostaLogin('ATENDENTE'));

    expect(router.navigateByUrl).toHaveBeenCalledWith('/clientes');
  });

  it('não deve redirecionar para endereço externo vindo do parâmetro retorno', () => {
    montar('//site-malicioso.teste');
    preencher('ana@oficina.teste', 'senha-ficticia');

    api.expectOne('/api/auth/login').flush(respostaLogin('ATENDENTE'));

    expect(router.navigateByUrl).toHaveBeenCalledWith('/quadro');
  });

  it('não deve chamar a API com formulário inválido', () => {
    montar();
    preencher('nao-e-email', '');

    api.expectNone('/api/auth/login');
    expect(fixture.componentInstance.form.invalid).toBeTrue();
  });

  it('deve mostrar a mensagem da API em outros erros', () => {
    montar();
    preencher('ana@oficina.teste', 'senha-ficticia');

    api.expectOne('/api/auth/login').flush(null, { status: 0, statusText: 'Unknown Error' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="erro-login"]').textContent).toContain('Sem conexão');
  });
});
