import { HttpErrorResponse } from '@angular/common/http';
import { errosDeCampo, textoDoErro } from './mensagem.service';

describe('tradução de erros da API', () => {
  it('deve usar a mensagem do corpo no formato padrão da API', () => {
    const erro = new HttpErrorResponse({
      status: 422,
      error: { status: 422, erro: 'Unprocessable Entity', mensagem: 'Desconto acima do permitido', campos: {} },
    });

    expect(textoDoErro(erro)).toBe('Desconto acima do permitido');
  });

  it('deve usar texto padrão pelo status quando não houver mensagem', () => {
    expect(textoDoErro(new HttpErrorResponse({ status: 403 }))).toBe('Você não tem permissão para esta ação.');
    expect(textoDoErro(new HttpErrorResponse({ status: 0 }))).toContain('Sem conexão');
    expect(textoDoErro(new HttpErrorResponse({ status: 418 }))).toBe('Erro inesperado. Tente novamente.');
    expect(textoDoErro(new Error('qualquer'))).toBe('Erro inesperado. Tente novamente.');
  });

  it('deve extrair os erros por campo de uma resposta 400', () => {
    const erro = new HttpErrorResponse({
      status: 400,
      error: { status: 400, erro: 'Bad Request', mensagem: 'Dados inválidos', campos: { documento: 'CPF/CNPJ inválido' } },
    });

    expect(errosDeCampo(erro)).toEqual({ documento: 'CPF/CNPJ inválido' });
    expect(errosDeCampo(new HttpErrorResponse({ status: 500 }))).toEqual({});
  });
});
