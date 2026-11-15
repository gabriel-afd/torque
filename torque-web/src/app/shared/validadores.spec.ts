import { FormControl } from '@angular/forms';
import {
  cnpjValido, cpfCnpjValidator, cpfValido, formatarDocumento, formatarTelefone, normalizarPlaca, placaValida,
  placaValidator, telefoneValidator,
} from './validadores';

// Documentos fictícios com dígito verificador válido (gerados só para teste).
const CPF_VALIDO = '529.982.247-25';
const CNPJ_VALIDO = '11.222.333/0001-81';

describe('validadores', () => {
  describe('CPF/CNPJ (RN03)', () => {
    it('deve aceitar CPF válido com ou sem máscara', () => {
      expect(cpfValido(CPF_VALIDO)).toBeTrue();
      expect(cpfValido('52998224725')).toBeTrue();
    });

    it('deve recusar CPF com dígito verificador errado, repetido ou incompleto', () => {
      expect(cpfValido('111.111.111-12')).toBeFalse();
      expect(cpfValido('529.982.247-26')).toBeFalse();
      expect(cpfValido('111.111.111-11')).toBeFalse();
      expect(cpfValido('5299822472')).toBeFalse();
    });

    it('deve aceitar CNPJ válido e recusar dígito errado', () => {
      expect(cnpjValido(CNPJ_VALIDO)).toBeTrue();
      expect(cnpjValido('11222333000181')).toBeTrue();
      expect(cnpjValido('11.222.333/0001-82')).toBeFalse();
      expect(cnpjValido('00.000.000/0000-00')).toBeFalse();
    });

    it('validator do formulário: erro cpfCnpj só para documento inválido', () => {
      expect(cpfCnpjValidator(new FormControl(CPF_VALIDO))).toBeNull();
      expect(cpfCnpjValidator(new FormControl(CNPJ_VALIDO))).toBeNull();
      expect(cpfCnpjValidator(new FormControl(''))).toBeNull();
      expect(cpfCnpjValidator(new FormControl('111.111.111-12'))).toEqual({ cpfCnpj: true });
      expect(cpfCnpjValidator(new FormControl('123'))).toEqual({ cpfCnpj: true });
    });

    it('deve mascarar CPF e CNPJ conforme o número de dígitos', () => {
      expect(formatarDocumento('52998224725')).toBe(CPF_VALIDO);
      expect(formatarDocumento('11222333000181')).toBe(CNPJ_VALIDO);
      expect(formatarDocumento('5299')).toBe('529.9');
      expect(formatarDocumento('abc529.982')).toBe('529.982');
      expect(formatarDocumento(null)).toBe('');
    });
  });

  describe('telefone', () => {
    it('deve exigir DDD e número (10 ou 11 dígitos)', () => {
      expect(telefoneValidator(new FormControl('(83) 99999-0001'))).toBeNull();
      expect(telefoneValidator(new FormControl('8333334444'))).toBeNull();
      expect(telefoneValidator(new FormControl('99999-0001'))).toEqual({ telefone: true });
    });

    it('deve mascarar fixo e celular', () => {
      expect(formatarTelefone('83999990001')).toBe('(83) 99999-0001');
      expect(formatarTelefone('8333334444')).toBe('(83) 3333-4444');
      expect(formatarTelefone('839')).toBe('(83) 9');
    });
  });

  describe('placa (RN02)', () => {
    it('deve aceitar os formatos antigo e Mercosul, com minúsculas e hífen', () => {
      expect(placaValida('ABC1234')).toBeTrue();
      expect(placaValida('ABC1D23')).toBeTrue();
      expect(placaValida('abc-1d23')).toBeTrue();
    });

    it('deve recusar placas fora dos formatos', () => {
      expect(placaValida('AB-1234')).toBeFalse();
      expect(placaValida('ABCD123')).toBeFalse();
      expect(placaValida('ABC12345')).toBeFalse();
      expect(placaValidator(new FormControl('ABCD123'))).toEqual({ placa: true });
      expect(placaValidator(new FormControl('ABC1D23'))).toBeNull();
    });

    it('deve normalizar para maiúsculas sem hífen e com no máximo 7 caracteres', () => {
      expect(normalizarPlaca('abc-1d23')).toBe('ABC1D23');
      expect(normalizarPlaca(' abc 1234 9')).toBe('ABC1234');
    });
  });
});
