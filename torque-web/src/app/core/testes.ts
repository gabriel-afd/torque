import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { EnvironmentProviders, LOCALE_ID, Provider } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MensagemService } from './mensagem.service';
import { Perfil } from './modelos';

/** JWT fictício (assinatura falsa) com a claim exp; só para testes do frontend. */
export function tokenFicticio(expiraEmSegundos = 3600, usuarioId = 1): string {
  const base64 = (obj: object) => btoa(JSON.stringify(obj)).replace(/=+$/, '');
  const exp = Math.floor(Date.now() / 1000) + expiraEmSegundos;
  return `${base64({ alg: 'HS256' })}.${base64({ sub: String(usuarioId), exp })}.assinatura-ficticia`;
}

export function respostaLogin(perfil: Perfil, nome = 'Ana Fictícia') {
  return { token: tokenFicticio(), nome, perfil };
}

registerLocaleData(localePt);

/** Providers comuns dos testes de componente: HTTP de teste, animações desligadas e locale pt-BR (como no app). */
export function provideTesteBase(): (Provider | EnvironmentProviders)[] {
  return [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), { provide: LOCALE_ID, useValue: 'pt-BR' }];
}

/** AuthService falso com o perfil (e o id) do usuário logado. */
export function authFalso(perfil: Perfil, usuarioId = 2) {
  return { perfil: () => perfil, usuarioId: () => usuarioId, temPerfil: (...perfis: Perfil[]) => perfis.includes(perfil) };
}

export function mensagensFalsas(): jasmine.SpyObj<MensagemService> {
  return jasmine.createSpyObj<MensagemService>('MensagemService', ['sucesso', 'aviso', 'erro']);
}
