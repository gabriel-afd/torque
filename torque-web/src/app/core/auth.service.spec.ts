import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService, CHAVE_SESSAO, lerExpiracao } from './auth.service';
import { respostaLogin, tokenFicticio } from './testes';

describe('AuthService', () => {
  let http: HttpTestingController;

  function criar(): AuthService {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.removeItem(CHAVE_SESSAO));
  afterEach(() => {
    http?.verify();
    localStorage.removeItem(CHAVE_SESSAO);
  });

  it('deve fazer login, guardar token e perfil em memória e no localStorage', () => {
    const auth = criar();
    const resposta = respostaLogin('ATENDENTE');

    auth.login('ana@oficina.teste', 'senha-ficticia').subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@oficina.teste', senha: 'senha-ficticia' });
    req.flush(resposta);

    expect(auth.autenticado()).toBeTrue();
    expect(auth.perfil()).toBe('ATENDENTE');
    expect(auth.token()).toBe(resposta.token);
    expect(JSON.parse(localStorage.getItem(CHAVE_SESSAO)!).perfil).toBe('ATENDENTE');
  });

  it('deve restaurar a sessão salva ao recarregar a página', () => {
    const token = tokenFicticio();
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify({ token, nome: 'Ana', perfil: 'MECANICO', expiraEm: lerExpiracao(token) }));

    const auth = criar();

    expect(auth.autenticado()).toBeTrue();
    expect(auth.perfil()).toBe('MECANICO');
  });

  it('deve descartar sessão salva com token expirado', () => {
    const token = tokenFicticio(-10);
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify({ token, nome: 'Ana', perfil: 'MECANICO', expiraEm: lerExpiracao(token) }));

    const auth = criar();

    expect(auth.autenticado()).toBeFalse();
    expect(auth.token()).toBeNull();
    expect(localStorage.getItem(CHAVE_SESSAO)).toBeNull();
  });

  it('deve limpar a sessão no logout', () => {
    const auth = criar();
    auth.login('ana@oficina.teste', 'x').subscribe();
    http.expectOne('/api/auth/login').flush(respostaLogin('ADMINISTRADOR'));

    auth.logout();

    expect(auth.autenticado()).toBeFalse();
    expect(auth.token()).toBeNull();
    expect(localStorage.getItem(CHAVE_SESSAO)).toBeNull();
  });

  it('deve ignorar conteúdo inválido no localStorage', () => {
    localStorage.setItem(CHAVE_SESSAO, '{ quebrado');

    expect(criar().autenticado()).toBeFalse();
  });

  it('deve ler a expiração da claim exp do token', () => {
    const exp = lerExpiracao(tokenFicticio(60))!;

    expect(exp).toBeGreaterThan(Date.now());
    expect(lerExpiracao('sem-pontos')).toBeNull();
  });
});
