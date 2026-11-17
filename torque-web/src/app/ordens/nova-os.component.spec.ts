import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { NovaOsComponent } from './nova-os.component';

describe('NovaOsComponent', () => {
  let fixture: ComponentFixture<NovaOsComponent>;
  let api: HttpTestingController;
  let dialogo: jasmine.SpyObj<MatDialogRef<NovaOsComponent>>;

  beforeEach(() => {
    dialogo = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [NovaOsComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogo },
        { provide: AuthService, useValue: { temPerfil: () => true } },
        { provide: MensagemService, useValue: jasmine.createSpyObj('MensagemService', ['sucesso', 'erro', 'aviso']) },
      ],
    });
    fixture = TestBed.createComponent(NovaOsComponent);
    api = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => api.verify());

  it('deve abrir a OS para o veículo encontrado pela placa', () => {
    const campo: HTMLInputElement = fixture.nativeElement.querySelector('[data-testid="busca-placa"]');
    campo.value = 'ABC1D23';
    campo.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('[data-testid="buscar-placa"]').click();
    api.expectOne((r) => r.url === '/api/veiculos').flush({
      id: 3, placa: 'ABC1D23', marca: 'Fiat', modelo: 'Uno', ano: 2015, cor: null,
      dono: { id: 9, nome: 'Joana Fictícia', ativo: true },
    });
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ problemaRelatado: ' Barulho na suspensão ', quilometragem: 85000 });
    fixture.componentInstance.abrir();

    const req = api.expectOne('/api/ordens');
    expect(req.request.body).toEqual({ veiculoId: 3, problemaRelatado: 'Barulho na suspensão', quilometragem: 85000 });
    req.flush({ id: 77, numero: 1077 });
    expect(dialogo.close).toHaveBeenCalledWith({ id: 77, numero: 1077 });
  });

  it('não deve permitir abrir OS sem veículo', () => {
    fixture.componentInstance.abrir();

    api.expectNone('/api/ordens');
    expect(fixture.nativeElement.querySelector('[data-testid="abrir-os"]').disabled).toBeTrue();
  });

  describe('com o veículo já escolhido (histórico do veículo)', () => {
    it('não deve pedir a placa e deve abrir a OS para o veículo recebido', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [NovaOsComponent],
        providers: [
          provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
          { provide: MatDialogRef, useValue: dialogo },
          { provide: MAT_DIALOG_DATA, useValue: { veiculo: {
            id: 9, placa: 'MZK2310', marca: 'VW', modelo: 'Gol', ano: 2014, cor: null, dono: { id: 4, nome: 'Pedro Fictício', ativo: true },
          } } },
          { provide: AuthService, useValue: { temPerfil: () => true } },
          { provide: MensagemService, useValue: jasmine.createSpyObj('MensagemService', ['sucesso', 'erro', 'aviso']) },
        ],
      });
      const comVeiculo = TestBed.createComponent(NovaOsComponent);
      const http = TestBed.inject(HttpTestingController);
      comVeiculo.detectChanges();

      expect(comVeiculo.nativeElement.querySelector('[data-testid="busca-placa"]')).toBeNull();
      expect(comVeiculo.nativeElement.querySelector('[data-testid="veiculo-escolhido"]').textContent).toContain('MZK2310');

      comVeiculo.componentInstance.form.setValue({ problemaRelatado: 'Revisão', quilometragem: null });
      comVeiculo.componentInstance.abrir();
      http.expectOne('/api/ordens').flush({ id: 78, numero: 1078 });
      http.verify();
      expect(dialogo.close).toHaveBeenCalledWith({ id: 78, numero: 1078 });
    });
  });
});
