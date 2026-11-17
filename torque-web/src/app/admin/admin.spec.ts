import { mensagensFalsas, provideTesteBase } from '../core/testes';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { pagina } from '../ordens/testes-ordens';
import { Usuario } from './admin.modelos';
import { PainelComponent } from './painel.component';
import { UsuarioFormComponent } from './usuario-form.component';
import { UsuariosComponent } from './usuarios.component';


const usuarios: Usuario[] = [
  { id: 1, nome: 'Admin Fictício', email: 'admin@oficina.teste', perfil: 'ADMINISTRADOR', ativo: true },
  { id: 5, nome: 'Raimundo Fictício', email: 'rai@oficina.teste', perfil: 'MECANICO', ativo: true },
  { id: 6, nome: 'Ex-atendente', email: 'ex@oficina.teste', perfil: 'ATENDENTE', ativo: false },
];

describe('Administração', () => {
  let api: HttpTestingController;
  let mensagens: jasmine.SpyObj<MensagemService>;

  function configurar(extras: unknown[] = []) {
    mensagens = mensagensFalsas();
    TestBed.configureTestingModule({
      providers: [
        ...provideTesteBase(),
        { provide: MensagemService, useValue: mensagens },
        ...(extras as never[]),
      ],
    });
  }

  afterEach(() => api.verify());

  describe('UsuariosComponent', () => {
    let fixture: ComponentFixture<UsuariosComponent>;
    let dialogo: jasmine.SpyObj<MatDialog>;

    function montar(resposta: unknown = true) {
      dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
      dialogo.open.and.returnValue({ afterClosed: () => of(resposta) } as never);
      configurar();
      TestBed.overrideProvider(MatDialog, { useValue: dialogo });
      fixture = TestBed.createComponent(UsuariosComponent);
      api = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      api.expectOne((r) => r.url === '/api/usuarios').flush(pagina(usuarios));
      fixture.detectChanges();
    }

    const linhas = (): string[] =>
      Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[data-testid="usuario"]')).map((l) => l.textContent!.replace(/\s+/g, ' '));

    it('deve listar usuários com e-mail, perfil e situação, sem senha', () => {
      montar();

      expect(linhas().length).toBe(3);
      expect(linhas()[1]).toContain('rai@oficina.teste');
      expect(linhas()[1]).toContain('Mecânico');
      expect(linhas()[2]).toContain('Inativo');
      expect(fixture.nativeElement.textContent.toLowerCase()).not.toContain('senha');
      expect(fixture.nativeElement.querySelectorAll('[data-testid="inativar"]').length).toBe(2);
    });

    it('deve inativar usuário após confirmação', () => {
      montar();

      (fixture.nativeElement.querySelectorAll('[data-testid="inativar"]')[1] as HTMLElement).click();

      const req = api.expectOne('/api/usuarios/5/inativar');
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...usuarios[1], ativo: false });
      api.expectOne((r) => r.url === '/api/usuarios').flush(pagina(usuarios));
      expect(mensagens.sucesso).toHaveBeenCalled();
    });

    it('deve mostrar a mensagem da API ao tentar inativar o último administrador', () => {
      montar();

      (fixture.nativeElement.querySelector('[data-testid="inativar"]') as HTMLElement).click();
      api.expectOne('/api/usuarios/1/inativar').flush(
        { status: 422, erro: 'Regra de negócio', mensagem: 'O sistema precisa de pelo menos um administrador ativo', campos: {} },
        { status: 422, statusText: 'Unprocessable Entity' },
      );

      expect(mensagens.erro).toHaveBeenCalled();
      expect(mensagens.sucesso).not.toHaveBeenCalled();
    });

    it('deve abrir o formulário de edição com o usuário escolhido', () => {
      montar(false);

      (fixture.nativeElement.querySelectorAll('[data-testid="editar"]')[1] as HTMLElement).click();

      expect(dialogo.open).toHaveBeenCalledWith(UsuarioFormComponent, jasmine.objectContaining({ data: { usuario: usuarios[1] } }));
    });
  });

  describe('UsuarioFormComponent', () => {
    let fixture: ComponentFixture<UsuarioFormComponent>;
    let fechar: jasmine.Spy;

    function montar(usuario?: Usuario) {
      fechar = jasmine.createSpy('close');
      configurar([{ provide: MAT_DIALOG_DATA, useValue: { usuario } }, { provide: MatDialogRef, useValue: { close: fechar } }]);
      fixture = TestBed.createComponent(UsuarioFormComponent);
      api = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    }

    it('deve cadastrar usuário com perfil e senha', () => {
      montar();
      fixture.componentInstance.form.setValue({ nome: ' Tiago Fictício ', email: 'tiago@oficina.teste', perfil: 'MECANICO', senha: 'senha-ficticia-1' });

      fixture.componentInstance.salvar();

      const req = api.expectOne('/api/usuarios');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ nome: 'Tiago Fictício', email: 'tiago@oficina.teste', perfil: 'MECANICO', senha: 'senha-ficticia-1' });
      req.flush({ id: 7, nome: 'Tiago Fictício', email: 'tiago@oficina.teste', perfil: 'MECANICO', ativo: true });
      expect(fechar).toHaveBeenCalled();
    });

    it('deve exigir senha de pelo menos 8 caracteres no cadastro', () => {
      montar();
      fixture.componentInstance.form.setValue({ nome: 'Tiago', email: 'tiago@oficina.teste', perfil: 'MECANICO', senha: '' });
      fixture.componentInstance.salvar();
      expect(fixture.componentInstance.form.controls.senha.hasError('required')).toBeTrue();

      fixture.componentInstance.form.controls.senha.setValue('curta');
      fixture.componentInstance.salvar();
      expect(fixture.componentInstance.form.controls.senha.hasError('minlength')).toBeTrue();
      api.expectNone('/api/usuarios');
    });

    it('deve editar perfil mantendo a senha quando o campo fica vazio', () => {
      montar(usuarios[1]);
      expect(fixture.componentInstance.form.controls.nome.value).toBe('Raimundo Fictício');
      fixture.componentInstance.form.controls.perfil.setValue('ATENDENTE');

      fixture.componentInstance.salvar();

      const req = api.expectOne('/api/usuarios/5');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ nome: 'Raimundo Fictício', email: 'rai@oficina.teste', perfil: 'ATENDENTE', senha: null });
      req.flush({ ...usuarios[1], perfil: 'ATENDENTE' });
    });

    it('deve mostrar e-mail duplicado (409) no campo e-mail', () => {
      montar();
      fixture.componentInstance.form.setValue({ nome: 'Outra', email: 'rai@oficina.teste', perfil: 'ATENDENTE', senha: 'senha-ficticia-1' });

      fixture.componentInstance.salvar();
      api.expectOne('/api/usuarios').flush(
        { status: 409, erro: 'Conflito', mensagem: 'Já existe um usuário com este e-mail', campos: {} },
        { status: 409, statusText: 'Conflict' },
      );

      expect(fixture.componentInstance.form.controls.email.getError('api')).toBe('Já existe um usuário com este e-mail');
    });
  });

  describe('PainelComponent', () => {
    let fixture: ComponentFixture<PainelComponent>;

    beforeEach(() => jasmine.clock().install().mockDate(new Date(2026, 10, 16, 10, 0)));
    afterEach(() => jasmine.clock().uninstall());

    function montar() {
      configurar();
      fixture = TestBed.createComponent(PainelComponent);
      api = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    }

    const texto = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`).textContent.trim();

    it('deve mostrar os números do mês atual', () => {
      montar();

      const req = api.expectOne((r) => r.url === '/api/relatorios/mes');
      expect(req.request.params.get('ano')).toBe('2026');
      expect(req.request.params.get('mes')).toBe('11');
      req.flush({ ano: 2026, mes: 11, abertas: 12, concluidas: 8, entregues: 7, faturamento: 4310.5 });
      fixture.detectChanges();

      expect(texto('mes')).toBe('novembro de 2026');
      expect(texto('abertas')).toBe('12');
      expect(texto('concluidas')).toBe('8');
      expect(texto('entregues')).toBe('7');
      expect(texto('faturamento')).toContain('4.310,50');
      expect(fixture.nativeElement.querySelector('[data-testid="mes-seguinte"]').disabled).toBeTrue();
    });

    it('deve trocar de mês pelo seletor, inclusive virando o ano', () => {
      montar();
      api.expectOne((r) => r.url === '/api/relatorios/mes').flush({ ano: 2026, mes: 11, abertas: 0, concluidas: 0, entregues: 0, faturamento: 0 });

      for (let i = 0; i < 11; i++) {
        fixture.componentInstance.mudarMes(-1);
        api.expectOne((r) => r.url === '/api/relatorios/mes').flush({ ano: 2026, mes: 1, abertas: 0, concluidas: 0, entregues: 0, faturamento: 0 });
      }
      fixture.detectChanges();

      expect(texto('mes')).toBe('dezembro de 2025');
      expect(fixture.nativeElement.querySelector('[data-testid="mes-seguinte"]').disabled).toBeFalse();
    });
  });
});
