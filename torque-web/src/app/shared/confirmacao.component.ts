import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export interface Confirmacao {
  titulo: string;
  mensagem: string;
  confirmar: string;
  /** Botão vermelho para ações destrutivas (cancelar OS, recusar orçamento). */
  perigo?: boolean;
}

/** Diálogo "tem certeza?" que fecha com true ao confirmar. */
@Component({
  selector: 'app-confirmacao',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content><p data-testid="mensagem-confirmacao">{{ dados.mensagem }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" [mat-dialog-close]="false">Voltar</button>
      <button mat-flat-button type="button" [color]="dados.perigo ? 'warn' : undefined"
              [class.acao-principal]="!dados.perigo" [mat-dialog-close]="true" data-testid="confirmar">
        {{ dados.confirmar }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmacaoComponent {
  readonly dados = inject<Confirmacao>(MAT_DIALOG_DATA);
}

/** Abre a confirmação e emite true só se o usuário confirmar. */
export function confirmar(dialogo: MatDialog, dados: Confirmacao): Observable<boolean> {
  return dialogo
    .open(ConfirmacaoComponent, { data: dados, width: '440px', maxWidth: '95vw' })
    .afterClosed()
    .pipe(map((resposta) => resposta === true));
}
