import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ErroApi } from './modelos';

const PADRAO_POR_STATUS: Record<number, string> = {
  0: 'Sem conexão com o servidor. Verifique a internet e tente novamente.',
  400: 'Confira os campos destacados.',
  401: 'Sua sessão expirou. Entre novamente.',
  403: 'Você não tem permissão para esta ação.',
  404: 'Registro não encontrado.',
  409: 'Os dados conflitam com um registro existente.',
  422: 'A operação não é permitida pelas regras da oficina.',
};

/** Mensagens rápidas (snackbar) para sucesso, avisos e erros no formato padrão da API. */
@Injectable({ providedIn: 'root' })
export class MensagemService {
  private readonly snackBar = inject(MatSnackBar);

  sucesso(texto: string): void {
    this.snackBar.open(texto, 'OK', { duration: 3000, panelClass: 'msg-sucesso' });
  }

  aviso(texto: string): void {
    this.snackBar.open(texto, 'OK', { duration: 5000, panelClass: 'msg-aviso' });
  }

  erro(erro: unknown): void {
    this.snackBar.open(textoDoErro(erro), 'Fechar', { duration: 6000, panelClass: 'msg-erro' });
  }
}

/** Mensagem da API (campo `mensagem`) ou um texto padrão pelo status HTTP. */
export function textoDoErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    const corpo = erro.error as Partial<ErroApi> | null;
    if (corpo && typeof corpo === 'object' && typeof corpo.mensagem === 'string' && corpo.mensagem) {
      return corpo.mensagem;
    }
    return PADRAO_POR_STATUS[erro.status] ?? 'Erro inesperado. Tente novamente.';
  }
  return 'Erro inesperado. Tente novamente.';
}

/** Erros por campo (`campos`) de uma resposta 400 da API. */
export function errosDeCampo(erro: unknown): Record<string, string> {
  if (erro instanceof HttpErrorResponse && erro.error && typeof erro.error === 'object') {
    const campos = (erro.error as Partial<ErroApi>).campos;
    return campos && typeof campos === 'object' ? campos : {};
  }
  return {};
}
