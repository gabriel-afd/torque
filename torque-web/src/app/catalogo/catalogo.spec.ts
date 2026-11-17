import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { pagina } from '../ordens/testes-ordens';
import { CatalogoFormComponent, DadosCatalogoForm } from './catalogo-form.component';
import { CatalogoListaComponent } from './catalogo-lista.component';
import { TipoCatalogo } from './catalogo.modelos';

registerLocaleData(localePt);

const servicos = [
  { id: 11, descricao: 'Alinhamento', precoMaoDeObra: 60, ativo: true },
  { id: 12, descricao: 'Balanceamento antigo', precoMaoDeObra: 40, ativo: false },
];
const pecas = [{ id: 21, codigo: 'PST-01', descricao: 'Jogo de pastilhas', precoVenda: 145, ativo: true }];

describe('Catálogo (admin)', () => {
  let api: HttpTestingController;
  let mensagens: jasmine.SpyObj<MensagemService>;

  function configurar(extras: unknown[] = []) {
    mensagens = jasmine.createSpyObj<MensagemService>('MensagemService', ['sucesso', 'aviso', 'erro']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: MensagemService, useValue: mensagens },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        ...(extras as never[]),
      ],
    });
  }

  afterEach(() => api.verify());

  describe('CatalogoListaComponent', () => {
    let fixture: ComponentFixture<CatalogoListaComponent>;
    let dialogo: jasmine.SpyObj<MatDialog>;

    function montar(tipo: TipoCatalogo, confirma: unknown = true) {
      dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
      dialogo.open.and.returnValue({ afterClosed: () => of(confirma) } as never);
      configurar();
      TestBed.overrideProvider(MatDialog, { useValue: dialogo });
      fixture = TestBed.createComponent(CatalogoListaComponent);
      api = TestBed.inject(HttpTestingController);
      fixture.componentRef.setInput('tipo', tipo);
      fixture.detectChanges();
    }

    const linhas = (): string[] =>
      Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[data-testid="item"]')).map((l) => l.textContent!.replace(/\s+/g, ' ').trim());

    it('deve listar os serviços com preço de mão de obra e situação', () => {
      montar('servicos');
      const req = api.expectOne((r) => r.url === '/api/servicos');
      expect(req.request.params.get('incluirInativos')).toBe('false');
      req.flush(pagina(servicos));
      fixture.detectChanges();

      expect(linhas()[0]).toContain('Alinhamento');
      expect(linhas()[0]).toContain('R$ 60,00');
      expect(linhas()[1]).toContain('Inativo');
      expect(fixture.nativeElement.querySelectorAll('[data-testid="inativar"]').length).toBe(1);
    });

    it('deve listar as peças com o código', () => {
      montar('pecas');
      api.expectOne((r) => r.url === '/api/pecas').flush(pagina(pecas));
      fixture.detectChanges();

      expect(linhas()[0]).toContain('PST-01');
      expect(linhas()[0]).toContain('R$ 145,00');
    });

    it('deve buscar e incluir inativos voltando para a primeira página', fakeAsync(() => {
      montar('servicos');
      api.expectOne((r) => r.url === '/api/servicos').flush(pagina(servicos));

      fixture.componentInstance.lista.pagina.set(2);
      fixture.componentInstance.incluirInativos.setValue(true);
      const comInativos = api.expectOne((r) => r.url === '/api/servicos');
      expect(comInativos.request.params.get('incluirInativos')).toBe('true');
      expect(comInativos.request.params.get('page')).toBe('0');
      comInativos.flush(pagina(servicos));

      fixture.componentInstance.busca.setValue('alin');
      tick(300);
      expect(api.expectOne((r) => r.url === '/api/servicos').request.params.get('busca')).toBe('alin');
    }));

    it('deve inativar após confirmação e recarregar a lista', () => {
      montar('servicos');
      api.expectOne((r) => r.url === '/api/servicos').flush(pagina(servicos));
      fixture.detectChanges();

      (fixture.nativeElement.querySelector('[data-testid="inativar"]') as HTMLElement).click();

      const req = api.expectOne('/api/servicos/11/inativar');
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...servicos[0], ativo: false });
      api.expectOne((r) => r.url === '/api/servicos').flush(pagina(servicos));
      expect(mensagens.sucesso).toHaveBeenCalled();
    });

    it('não deve inativar se o admin voltar na confirmação', () => {
      montar('servicos', false);
      api.expectOne((r) => r.url === '/api/servicos').flush(pagina(servicos));
      fixture.detectChanges();

      (fixture.nativeElement.querySelector('[data-testid="inativar"]') as HTMLElement).click();

      expect(dialogo.open).toHaveBeenCalled();
      api.expectNone('/api/servicos/11/inativar');
    });

    it('deve abrir o formulário de novo item e recarregar depois de salvar', () => {
      montar('pecas', { id: 22, codigo: 'X', descricao: 'Nova', preco: 1, ativo: true });
      api.expectOne((r) => r.url === '/api/pecas').flush(pagina(pecas));
      fixture.detectChanges();

      (fixture.nativeElement.querySelector('[data-testid="novo"]') as HTMLElement).click();

      expect(dialogo.open).toHaveBeenCalledWith(CatalogoFormComponent, jasmine.objectContaining({ data: { tipo: 'pecas', item: undefined } }));
      api.expectOne((r) => r.url === '/api/pecas').flush(pagina(pecas));
    });
  });

  describe('CatalogoFormComponent', () => {
    let fixture: ComponentFixture<CatalogoFormComponent>;
    let fechar: jasmine.Spy;

    function montar(dados: DadosCatalogoForm) {
      fechar = jasmine.createSpy('close');
      configurar([{ provide: MAT_DIALOG_DATA, useValue: dados }, { provide: MatDialogRef, useValue: { close: fechar } }]);
      fixture = TestBed.createComponent(CatalogoFormComponent);
      api = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    }

    it('deve cadastrar peça com código, descrição e preço de venda', () => {
      montar({ tipo: 'pecas' });
      fixture.componentInstance.form.setValue({ codigo: ' FLT-02 ', descricao: ' Filtro de ar ', preco: 39.9 });

      fixture.componentInstance.salvar();

      const req = api.expectOne('/api/pecas');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ codigo: 'FLT-02', descricao: 'Filtro de ar', precoVenda: 39.9 });
      req.flush({ id: 30, codigo: 'FLT-02', descricao: 'Filtro de ar', precoVenda: 39.9, ativo: true });
      expect(fechar).toHaveBeenCalledWith({ id: 30, codigo: 'FLT-02', descricao: 'Filtro de ar', preco: 39.9, ativo: true });
    });

    it('deve editar serviço enviando só descrição e mão de obra', () => {
      montar({ tipo: 'servicos', item: { id: 11, codigo: null, descricao: 'Alinhamento', preco: 60, ativo: true } });
      expect(fixture.nativeElement.querySelector('[data-testid="codigo"]')).toBeNull();
      fixture.componentInstance.form.controls.preco.setValue(70);

      fixture.componentInstance.salvar();

      const req = api.expectOne('/api/servicos/11');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ descricao: 'Alinhamento', precoMaoDeObra: 70 });
      req.flush({ ...servicos[0], precoMaoDeObra: 70 });
    });

    it('deve exigir código para peça e não aceitar preço negativo', () => {
      montar({ tipo: 'pecas' });
      fixture.componentInstance.form.setValue({ codigo: '', descricao: 'Filtro', preco: -1 });

      fixture.componentInstance.salvar();

      api.expectNone('/api/pecas');
      expect(fixture.componentInstance.form.controls.codigo.hasError('required')).toBeTrue();
      expect(fixture.componentInstance.form.controls.preco.hasError('min')).toBeTrue();
    });

    it('deve mostrar o código duplicado (409) no campo código', () => {
      montar({ tipo: 'pecas' });
      fixture.componentInstance.form.setValue({ codigo: 'PST-01', descricao: 'Outra', preco: 10 });

      fixture.componentInstance.salvar();
      api.expectOne('/api/pecas').flush(
        { status: 409, erro: 'Conflito', mensagem: 'Já existe uma peça com este código', campos: {} },
        { status: 409, statusText: 'Conflict' },
      );

      expect(fixture.componentInstance.form.controls.codigo.getError('api')).toBe('Já existe uma peça com este código');
      expect(mensagens.erro).not.toHaveBeenCalled();
    });

    it('deve mostrar o erro 400 do preço no campo preço', () => {
      montar({ tipo: 'servicos' });
      fixture.componentInstance.form.setValue({ codigo: '', descricao: 'Caro', preco: 1 });

      fixture.componentInstance.salvar();
      api.expectOne('/api/servicos').flush(
        { status: 400, erro: 'Validação', mensagem: 'Dados inválidos', campos: { precoMaoDeObra: 'valor numérico fora do limite' } },
        { status: 400, statusText: 'Bad Request' },
      );

      expect(fixture.componentInstance.form.controls.preco.getError('api')).toBe('valor numérico fora do limite');
    });
  });
});
