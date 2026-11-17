import { WritableSignal } from '@angular/core';
import { MonoTypeOperatorFunction, defer, finalize } from 'rxjs';

/**
 * Liga o indicador (carregando, salvando…) quando a requisição começa e desliga ao terminar, com sucesso
 * ou erro. Substitui o par `set(true)` / `set(false)` repetido em cada `next` e `error`.
 */
export function comCarregamento<T>(indicador: WritableSignal<boolean>): MonoTypeOperatorFunction<T> {
  return (origem) =>
    defer(() => {
      indicador.set(true);
      return origem.pipe(finalize(() => indicador.set(false)));
    });
}
