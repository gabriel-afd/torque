import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Só os dígitos de um texto (remove máscara). */
export function somenteDigitos(valor: string | null | undefined): string {
  return (valor ?? '').replace(/\D/g, '');
}

// CPF / CNPJ (RN03) ----------------------------------------------------------------

function todosIguais(digitos: string): boolean {
  return /^(\d)\1*$/.test(digitos);
}

export function cpfValido(valor: string): boolean {
  const cpf = somenteDigitos(valor);
  if (cpf.length !== 11 || todosIguais(cpf)) {
    return false;
  }
  for (const posicao of [9, 10]) {
    let soma = 0;
    for (let i = 0; i < posicao; i++) {
      soma += Number(cpf[i]) * (posicao + 1 - i);
    }
    if (((soma * 10) % 11) % 10 !== Number(cpf[posicao])) {
      return false;
    }
  }
  return true;
}

export function cnpjValido(valor: string): boolean {
  const cnpj = somenteDigitos(valor);
  if (cnpj.length !== 14 || todosIguais(cnpj)) {
    return false;
  }
  const digito = (pesos: number[]) => {
    const resto = pesos.reduce((soma, peso, i) => soma + Number(cnpj[i]) * peso, 0) % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return (
    digito([5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cnpj[12]) &&
    digito([6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cnpj[13])
  );
}

export function cpfCnpjValido(valor: string | null | undefined): boolean {
  const digitos = somenteDigitos(valor);
  return digitos.length === 11 ? cpfValido(digitos) : digitos.length === 14 ? cnpjValido(digitos) : false;
}

/** Erro `cpfCnpj` quando o documento não é um CPF ou CNPJ com dígito verificador válido. Vazio passa. */
export const cpfCnpjValidator: ValidatorFn = (controle: AbstractControl): ValidationErrors | null => {
  const valor = controle.value as string | null;
  if (!valor) {
    return null;
  }
  return cpfCnpjValido(valor) ? null : { cpfCnpj: true };
};

/** 000.000.000-00 ou 00.000.000/0000-00, conforme o número de dígitos digitados. */
export function formatarDocumento(valor: string | null | undefined): string {
  const d = somenteDigitos(valor).slice(0, 14);
  if (d.length <= 11) {
    return d
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
  }
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2');
}

// Telefone -----------------------------------------------------------------------

/** DDD + número: 10 ou 11 dígitos. Vazio passa (use `required` junto). */
export const telefoneValidator: ValidatorFn = (controle: AbstractControl): ValidationErrors | null => {
  const valor = controle.value as string | null;
  if (!valor) {
    return null;
  }
  const digitos = somenteDigitos(valor).length;
  return digitos === 10 || digitos === 11 ? null : { telefone: true };
};

/** (00) 0000-0000 ou (00) 00000-0000. */
export function formatarTelefone(valor: string | null | undefined): string {
  const d = somenteDigitos(valor).slice(0, 11);
  if (d.length <= 2) {
    return d.length ? `(${d}` : '';
  }
  const ddd = d.slice(0, 2);
  const numero = d.slice(2);
  const corte = numero.length > 8 ? 5 : 4;
  return numero.length > corte ? `(${ddd}) ${numero.slice(0, corte)}-${numero.slice(corte)}` : `(${ddd}) ${numero}`;
}

// Placa (RN02) -------------------------------------------------------------------

const FORMATO_PLACA = /^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/;

/** Maiúsculas, sem hífen, sem espaços e com no máximo 7 caracteres. */
export function normalizarPlaca(valor: string | null | undefined): string {
  return (valor ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
}

/** Antiga ABC1234 ou Mercosul ABC1D23 (aceita minúsculas e hífen). */
export function placaValida(valor: string | null | undefined): boolean {
  return FORMATO_PLACA.test((valor ?? '').toUpperCase().replace(/[\s-]/g, ''));
}

export const placaValidator: ValidatorFn = (controle: AbstractControl): ValidationErrors | null => {
  const valor = controle.value as string | null;
  if (!valor) {
    return null;
  }
  return placaValida(valor) ? null : { placa: true };
};
