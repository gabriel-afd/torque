import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { AuthService, CHAVE_SESSAO } from '../core/auth.service';
import { Perfil } from '../core/modelos';
import { respostaLogin } from '../core/testes';
import { LayoutComponent, secaoDaUrl } from './layout.component';

describe('LayoutComponent', () => {
  beforeEach(() => localStorage.removeItem(CHAVE_SESSAO));
  afterEach(() => localStorage.removeItem(CHAVE_SESSAO));

  function montar(perfil: Perfil, nome = 'Ana Fictícia') {
    TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    });
    const auth = TestBed.inject(AuthService);
    auth.login('ana@oficina.teste', 'x').subscribe();
    TestBed.inject(HttpTestingController).expectOne('/api/auth/login').flush(respostaLogin(perfil, nome));
    const fixture = TestBed.createComponent(LayoutComponent);
    fixture.detectChanges();
    return fixture;
  }

  function itensDoMenu(elemento: HTMLElement): string[] {
    return Array.from(elemento.querySelectorAll('[data-testid="item-menu"]')).map((a) => a.textContent!.trim());
  }

  // CT52
  it('não deve mostrar Usuários, Catálogo e Painel para ATENDENTE', () => {
    const itens = itensDoMenu(montar('ATENDENTE').nativeElement);

    expect(itens).toEqual(['Quadro de OS', 'Clientes', 'Veículos']);
    expect(itens).not.toContain('Usuários');
    expect(itens).not.toContain('Catálogo');
    expect(itens).not.toContain('Painel');
  });

  it('deve mostrar todos os itens para ADMINISTRADOR', () => {
    expect(itensDoMenu(montar('ADMINISTRADOR').nativeElement)).toEqual([
      'Quadro de OS', 'Clientes', 'Veículos', 'Catálogo', 'Painel', 'Usuários',
    ]);
  });

  it('deve mostrar ao MECANICO só Quadro de OS e Veículos', () => {
    expect(itensDoMenu(montar('MECANICO').nativeElement)).toEqual(['Quadro de OS', 'Veículos']);
  });

  it('deve mostrar nome, perfil e iniciais do usuário', () => {
    const elemento: HTMLElement = montar('ATENDENTE', 'Carla Souza Fictícia').nativeElement;

    expect(elemento.querySelector('.nome')!.textContent).toContain('Carla Souza Fictícia');
    expect(elemento.querySelector('.nome')!.textContent).toContain('Atendente');
    expect(elemento.querySelector('.avatar')!.textContent!.trim()).toBe('CS');
  });

  it('deve sair: limpar a sessão e ir para o login', () => {
    const fixture = montar('ATENDENTE');
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture.componentInstance.sair();

    expect(TestBed.inject(AuthService).autenticado()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('deve mostrar o crédito do autor no menu lateral', () => {
    const elemento: HTMLElement = montar('MECANICO').nativeElement;

    expect(elemento.querySelector('.lateral [data-testid="credito"]')!.textContent!.replace(/\s+/g, ' ').trim())
      .toBe('Desenvolvido por Gabriel Medeiros de Mendonça');
  });

  it('deve mostrar o ícone de cada item do menu', () => {
    const elemento: HTMLElement = montar('ADMINISTRADOR').nativeElement;
    const icones = Array.from(elemento.querySelectorAll('[data-testid="item-menu"] .icone')).map(
      (i) => (i as HTMLElement).style.getPropertyValue('--icone'),
    );

    expect(icones).toEqual([
      "url('/icones/quadro.svg')", "url('/icones/clientes.svg')", "url('/icones/veiculos.svg')",
      "url('/icones/catalogo.svg')", "url('/icones/painel.svg')", "url('/icones/usuarios.svg')",
    ]);
  });

  it('deve montar a trilha "Oficina / …" pela URL', () => {
    expect(secaoDaUrl('/quadro')).toBe('Ordens de serviço');
    expect(secaoDaUrl('/ordens/1001')).toBe('Ordens de serviço');
    expect(secaoDaUrl('/clientes/7?aba=veiculos')).toBe('Clientes');
    expect(secaoDaUrl('/veiculos/ABC1D23')).toBe('Veículos');
    expect(secaoDaUrl('/admin/usuarios')).toBe('Usuários');
  });
});
