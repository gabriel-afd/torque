import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { MensagemService } from './mensagem.service';
import { Perfil } from './modelos';

/** Só entra logado; senão vai para /login lembrando a página pedida. */
export const autenticadoGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  if (auth.token()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { retorno: estado.url } });
};

/** Login não faz sentido para quem já está logado. */
export const anonimoGuard: CanActivateFn = () =>
  inject(AuthService).token() ? inject(Router).createUrlTree(['/quadro']) : true;

/** Só para os perfis informados; os demais voltam ao quadro com aviso. */
export function perfilGuard(...perfis: Perfil[]): CanMatchFn {
  return () => {
    const auth = inject(AuthService);
    if (auth.token() && auth.temPerfil(...perfis)) {
      return true;
    }
    if (auth.token()) {
      inject(MensagemService).aviso('Você não tem permissão para acessar esta página.');
    }
    return inject(Router).createUrlTree([auth.token() ? '/quadro' : '/login']);
  };
}
