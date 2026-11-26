import { TestBed } from '@angular/core/testing';
import { appConfig } from './app.config';
import { LoginComponent } from './login/login.component';

describe('appConfig (padrões dos campos de formulário)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [LoginComponent], providers: [...appConfig.providers] });
  });

  it('deve reservar a linha da mensagem de erro em cada campo, sem um campo encostar no outro', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const campos: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('mat-form-field'));
    expect(campos.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.mat-mdc-form-field-subscript-dynamic-size')).toBeNull();

    const [email, senha] = campos.map((c) => c.getBoundingClientRect());
    const contornoEmail = campos[0].querySelector('.mdc-notched-outline')!.getBoundingClientRect();
    expect(contornoEmail.bottom + 16).toBeLessThanOrEqual(senha.top + 0.5);
    expect(email.bottom).toBeLessThanOrEqual(senha.top + 0.5);
  });
});
