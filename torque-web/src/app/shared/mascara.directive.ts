import { Directive, HostListener, OnInit, inject, input } from '@angular/core';
import { NgControl } from '@angular/forms';
import { formatarDocumento, formatarTelefone, normalizarPlaca } from './validadores';

export type TipoMascara = 'documento' | 'telefone' | 'placa';

const FORMATADORES: Record<TipoMascara, (valor: string) => string> = {
  documento: formatarDocumento,
  telefone: formatarTelefone,
  placa: normalizarPlaca,
};

/**
 * Máscara de digitação: `<input formControlName="documento" appMascara="documento">`.
 * O valor do formulário fica formatado; a API aceita a máscara e normaliza.
 */
@Directive({
  selector: 'input[appMascara]',
  standalone: true,
})
export class MascaraDirective implements OnInit {
  readonly appMascara = input.required<TipoMascara>();
  private readonly controle = inject(NgControl, { self: true });

  ngOnInit(): void {
    this.aplicar();
  }

  @HostListener('input')
  @HostListener('blur')
  aplicar(): void {
    const controle = this.controle.control;
    if (!controle) {
      return;
    }
    const atual = (controle.value as string | null) ?? '';
    const formatado = FORMATADORES[this.appMascara()](atual);
    if (formatado !== atual) {
      controle.setValue(formatado);
    }
  }
}
