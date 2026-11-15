import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Route, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { anonimoGuard, autenticadoGuard, perfilGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { MensagemService } from './mensagem.service';
import { Perfil } from './modelos';

describe('guards de rota', () => {
  let token: string | null;
  let perfil: Perfil | null;
  let mensagens: jasmine.SpyObj<MensagemService>;

  beforeEach(() => {
    token = null;
    perfil = null;
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['aviso']);
    const auth: Partial<AuthService> = {
      token: () => token,
      temPerfil: (...perfis: Perfil[]) => perfil !== null && perfis.includes(perfil),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }, { provide: MensagemService, useValue: mensagens }],
    });
  });

  const estado = (url: string) => ({ url }) as RouterStateSnapshot;
  const rota = {} as ActivatedRouteSnapshot;
  const url = (resultado: unknown) => TestBed.inject(Router).serializeUrl(resultado as UrlTree);

  // CT48
  it('deve redirecionar para /login quem acessa /quadro sem token', () => {
    const resultado = TestBed.runInInjectionContext(() => autenticadoGuard(rota, estado('/quadro')));

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(url(resultado)).toBe('/login?retorno=%2Fquadro');
  });

  it('deve liberar a rota para usuário logado', () => {
    token = 'token-ficticio';

    expect(TestBed.runInInjectionContext(() => autenticadoGuard(rota, estado('/quadro')))).toBeTrue();
  });

  it('deve mandar para o quadro quem já está logado e abre o login', () => {
    token = 'token-ficticio';

    expect(url(TestBed.runInInjectionContext(() => anonimoGuard(rota, estado('/login'))))).toBe('/quadro');
  });

  it('deve liberar rota de administrador só para ADMINISTRADOR', () => {
    token = 'token-ficticio';
    perfil = 'ADMINISTRADOR';

    expect(TestBed.runInInjectionContext(() => perfilGuard('ADMINISTRADOR')({} as Route, []))).toBeTrue();
  });

  it('deve barrar ATENDENTE na rota de administrador, com aviso e volta ao quadro', () => {
    token = 'token-ficticio';
    perfil = 'ATENDENTE';

    const resultado = TestBed.runInInjectionContext(() => perfilGuard('ADMINISTRADOR')({} as Route, []));

    expect(url(resultado)).toBe('/quadro');
    expect(mensagens.aviso).toHaveBeenCalled();
  });

  it('deve mandar para o login quem não está logado e tenta rota por perfil', () => {
    const resultado = TestBed.runInInjectionContext(() => perfilGuard('ADMINISTRADOR')({} as Route, []));

    expect(url(resultado)).toBe('/login');
    expect(mensagens.aviso).not.toHaveBeenCalled();
  });
});
