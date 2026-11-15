import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { MensagemService } from './mensagem.service';

const ROTA_LOGIN = '/api/auth/login';
export const AVISO_SESSAO_EXPIRADA = 'Sua sessão expirou. Entre novamente.';

/**
 * Envia o token (Authorization: Bearer) nas chamadas à API e, se a API responder 401 com o usuário logado,
 * encerra a sessão e volta ao login com aviso.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const mensagens = inject(MensagemService);

  const ehApi = req.url.startsWith('/api/');
  const ehLogin = req.url === ROTA_LOGIN;
  const token = ehApi && !ehLogin ? auth.token() : null;
  const requisicao = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401 && ehApi && !ehLogin) {
        auth.logout();
        mensagens.aviso(AVISO_SESSAO_EXPIRADA);
        const atual = router.url;
        router.navigate(['/login'], atual.startsWith('/login') ? {} : { queryParams: { retorno: atual } });
      }
      return throwError(() => erro);
    }),
  );
};
