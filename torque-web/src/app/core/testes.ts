import { Perfil } from './modelos';

/** JWT fictício (assinatura falsa) com a claim exp; só para testes do frontend. */
export function tokenFicticio(expiraEmSegundos = 3600): string {
  const base64 = (obj: object) => btoa(JSON.stringify(obj)).replace(/=+$/, '');
  const exp = Math.floor(Date.now() / 1000) + expiraEmSegundos;
  return `${base64({ alg: 'HS256' })}.${base64({ sub: '1', exp })}.assinatura-ficticia`;
}

export function respostaLogin(perfil: Perfil, nome = 'Ana Fictícia') {
  return { token: tokenFicticio(), nome, perfil };
}
