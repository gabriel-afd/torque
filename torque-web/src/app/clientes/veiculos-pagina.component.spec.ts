import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { Perfil } from '../core/modelos';
import { authFalso, provideTesteBase } from '../core/testes';
import { CadastroRapidoComponent } from './cadastro-rapido.component';
import { VeiculosPaginaComponent } from './veiculos-pagina.component';

describe('VeiculosPaginaComponent', () => {
  let fixture: ComponentFixture<VeiculosPaginaComponent>;
  let dialogo: jasmine.SpyObj<MatDialog>;
  let router: Router;

  function montar(perfil: Perfil, cadastrado?: object) {
    dialogo = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogo.open.and.returnValue({ afterClosed: () => of(cadastrado) } as never);
    TestBed.configureTestingModule({
      imports: [VeiculosPaginaComponent],
      providers: [provideRouter([]), ...provideTesteBase(), { provide: AuthService, useValue: authFalso(perfil) }],
    });
    TestBed.overrideProvider(MatDialog, { useValue: dialogo });
    fixture = TestBed.createComponent(VeiculosPaginaComponent);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture.detectChanges();
  }

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('"Novo veículo" deve abrir o cadastro rápido sem placa e ir para o histórico do veículo cadastrado', () => {
    montar('ATENDENTE', { placa: 'QWE4R56' });

    fixture.nativeElement.querySelector('[data-testid="novo-veiculo"]').click();

    expect(dialogo.open).toHaveBeenCalledWith(CadastroRapidoComponent, jasmine.objectContaining({ data: { placa: '' } }));
    expect(router.navigate).toHaveBeenCalledWith(['/veiculos', 'QWE4R56']);
  });

  it('não deve navegar quando o cadastro for cancelado', () => {
    montar('ADMINISTRADOR');

    fixture.nativeElement.querySelector('[data-testid="novo-veiculo"]').click();

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('não deve mostrar "Novo veículo" ao MECANICO', () => {
    montar('MECANICO');

    expect(fixture.nativeElement.querySelector('[data-testid="novo-veiculo"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="busca-placa"]')).not.toBeNull();
  });
});
