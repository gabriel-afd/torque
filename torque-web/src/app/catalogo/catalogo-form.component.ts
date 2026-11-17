import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { errosDeCampo, MensagemService } from '../core/mensagem.service';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { ItemCatalogo, ROTULO_TIPO, TipoCatalogo } from './catalogo.modelos';
import { CatalogoService } from './catalogo.service';

export interface DadosCatalogoForm {
  tipo: TipoCatalogo;
  item?: ItemCatalogo;
}

/** Cadastro e edição de serviço ou peça (diálogo). Fecha devolvendo o item salvo. */
@Component({
  selector: 'app-catalogo-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ dados.item ? 'Editar' : 'Novo' }} {{ rotulo }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="salvar()" id="form-catalogo" class="campos" novalidate>
        @if (dados.tipo === 'pecas') {
          <mat-form-field>
            <mat-label>Código</mat-label>
            <input matInput formControlName="codigo" maxlength="40" data-testid="codigo" />
            @if (form.controls.codigo.hasError('required')) { <mat-error>Informe o código</mat-error> }
            @if (form.controls.codigo.hasError('api')) { <mat-error>{{ form.controls.codigo.getError('api') }}</mat-error> }
          </mat-form-field>
        }
        <mat-form-field>
          <mat-label>Descrição</mat-label>
          <input matInput formControlName="descricao" maxlength="150" data-testid="descricao" />
          @if (form.controls.descricao.hasError('required')) { <mat-error>Informe a descrição</mat-error> }
          @if (form.controls.descricao.hasError('api')) { <mat-error>{{ form.controls.descricao.getError('api') }}</mat-error> }
        </mat-form-field>
        <mat-form-field>
          <mat-label>{{ dados.tipo === 'pecas' ? 'Preço de venda (R$)' : 'Mão de obra (R$)' }}</mat-label>
          <input matInput type="number" min="0" step="0.01" formControlName="preco" data-testid="preco" />
          @if (form.controls.preco.hasError('required')) { <mat-error>Informe o preço</mat-error> }
          @if (form.controls.preco.hasError('min')) { <mat-error>O preço não pode ser negativo</mat-error> }
          @if (form.controls.preco.hasError('api')) { <mat-error>{{ form.controls.preco.getError('api') }}</mat-error> }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="acao-principal" type="submit" form="form-catalogo" [disabled]="salvando()" data-testid="salvar">
        Salvar
      </button>
    </mat-dialog-actions>
  `,
  styles: `.campos { display: grid; gap: 4px; min-width: min(420px, 80vw); }`,
})
export class CatalogoFormComponent {
  readonly dados = inject<DadosCatalogoForm>(MAT_DIALOG_DATA);
  private readonly api = inject(CatalogoService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<CatalogoFormComponent, ItemCatalogo>);

  readonly rotulo = ROTULO_TIPO[this.dados.tipo].singular;
  readonly salvando = signal(false);
  readonly form = inject(FormBuilder).group({
    codigo: [this.dados.item?.codigo ?? '', this.dados.tipo === 'pecas' ? [Validators.required, Validators.maxLength(40)] : []],
    descricao: [this.dados.item?.descricao ?? '', [Validators.required, Validators.maxLength(150)]],
    preco: [this.dados.item?.preco ?? (null as number | null), [Validators.required, Validators.min(0)]],
  });

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { codigo, descricao, preco } = this.form.getRawValue();
    const requisicao = { codigo: codigo || null, descricao: descricao ?? '', preco: Number(preco) };
    const { tipo, item } = this.dados;
    this.salvando.set(true);
    (item ? this.api.atualizar(tipo, item.id, requisicao) : this.api.cadastrar(tipo, requisicao)).subscribe({
      next: (salvo) => {
        this.salvando.set(false);
        this.mensagens.sucesso(`${this.rotulo[0].toUpperCase()}${this.rotulo.slice(1)} salvo.`);
        this.dialogo.close(salvo);
      },
      error: (erro) => {
        this.salvando.set(false);
        if (!this.aplicarErros(erro)) {
          this.mensagens.erro(erro);
        }
      },
    });
  }

  /** 400 por campo (o preço vem como precoVenda/precoMaoDeObra) e 409 de código duplicado. */
  private aplicarErros(erro: unknown): boolean {
    let aplicou = aplicarErrosDaApi(this.form, erro, { codigo: /código/i });
    const campos = errosDeCampo(erro);
    const erroPreco = campos['precoVenda'] ?? campos['precoMaoDeObra'];
    if (erroPreco) {
      this.form.controls.preco.setErrors({ api: erroPreco });
      this.form.controls.preco.markAsTouched();
      aplicou = true;
    }
    return aplicou;
  }
}
