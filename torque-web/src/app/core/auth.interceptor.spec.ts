import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AVISO_SESSAO_EXPIRADA, authInterceptor } from './auth.interceptor';
import { AuthService, CHAVE_SESSAO } from './auth.service';
import { MensagemService } from './mensagem.service';
import { respostaLogin } from './testes';

describe('authInterceptor', () => {
  let http: HttpClient;
  let api: HttpTestingController;
  let auth: AuthService;
  let router: Router;
  let mensagens: jasmine.SpyObj<MensagemService>;

  beforeEach(() => {
    localStorage.removeItem(CHAVE_SESSAO);
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['aviso', 'erro', 'sucesso']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MensagemService, useValue: mensagens },
      ],
    });
    http = TestBed.inject(HttpClient);
    api = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    api.verify();
    localStorage.removeItem(CHAVE_SESSAO);
  });

  function logar(): string {
    const resposta = respostaLogin('ATENDENTE');
    auth.login('ana@oficina.teste', 'x').subscribe();
    api.expectOne('/api/auth/login').flush(resposta);
    return resposta.token;
  }

  // CT49
  it('deve enviar Authorization: Bearer <token> nas chamadas à API com usuário logado', () => {
    const token = logar();

    http.get('/api/ordens').subscribe();

    const req = api.expectOne('/api/ordens');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush({});
  });

  it('não deve enviar Authorization sem usuário logado', () => {
    http.get('/api/ordens').subscribe({ error: () => undefined });

    const req = api.expectOne('/api/ordens');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('não deve enviar o token para endereços fora da API', () => {
    logar();

    http.get('https://exemplo.teste/arquivo.json').subscribe();

    const req = api.expectOne('https://exemplo.teste/arquivo.json');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  // CT50
  it('deve voltar ao login com aviso quando a API responder 401 (token expirado)', () => {
    logar();
    let recebeuErro = false;

    http.get('/api/ordens').subscribe({ error: () => (recebeuErro = true) });
    api.expectOne('/api/ordens').flush(
      { status: 401, erro: 'Unauthorized', mensagem: 'Faça login', campos: {} },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(recebeuErro).toBeTrue();
    expect(auth.autenticado()).toBeFalse();
    expect(mensagens.aviso).toHaveBeenCalledWith(AVISO_SESSAO_EXPIRADA);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], jasmine.anything());
  });

  it('não deve tratar 401 do próprio login como sessão expirada', () => {
    auth.login('ana@oficina.teste', 'errada').subscribe({ error: () => undefined });
    api.expectOne('/api/auth/login').flush(
      { status: 401, erro: 'Unauthorized', mensagem: 'E-mail ou senha inválidos', campos: {} },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(mensagens.aviso).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('não deve deslogar em 403', () => {
    logar();

    http.get('/api/usuarios').subscribe({ error: () => undefined });
    api.expectOne('/api/usuarios').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(auth.autenticado()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
