import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';
import { errosDeCampo } from '../core/mensagem.service';
import { ErroApi } from '../core/modelos';

/** Para um 409, qual campo do formulário a mensagem da API se refere (ex.: "CPF/CNPJ" → documento). */
export type MapaConflitos = Record<string, RegExp>;

/**
 * Coloca os erros da API nos campos do formulário: 400 pelos `campos` da resposta, 409 pelo texto da
 * `mensagem`. Devolve true se algum campo recebeu o erro (senão a tela mostra a mensagem geral).
 */
export function aplicarErrosDaApi(form: FormGroup, erro: unknown, conflitos: MapaConflitos = {}): boolean {
  if (!(erro instanceof HttpErrorResponse)) {
    return false;
  }
  let aplicou = false;
  const marcar = (campo: string, mensagem: string) => {
    const controle = form.get(campo);
    if (controle) {
      controle.setErrors({ ...(controle.errors ?? {}), api: mensagem });
      controle.markAsTouched();
      aplicou = true;
    }
  };

  if (erro.status === 400) {
    Object.entries(errosDeCampo(erro)).forEach(([campo, mensagem]) => marcar(campo, mensagem));
  } else if (erro.status === 409) {
    const mensagem = (erro.error as Partial<ErroApi> | null)?.mensagem ?? '';
    Object.entries(conflitos).forEach(([campo, padrao]) => {
      if (padrao.test(mensagem)) {
        marcar(campo, mensagem);
      }
    });
  }
  return aplicou;
}
