import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MensagemService } from '../core/mensagem.service';
import { BuscaPlacaComponent } from '../clientes/busca-placa.component';
import { VeiculoComDono } from '../clientes/clientes.modelos';
import { aplicarErrosDaApi } from '../shared/erros-formulario';
import { OrdemCriada } from './ordens.modelos';
import { OrdensService } from './ordens.service';

/**
 * "+ Nova OS": usa o veículo recebido em `data.veiculo` ou busca a placa (ou cadastra veículo/cliente na hora) e abre a OS com o problema relatado.
 * Fecha devolvendo a OS criada.
 */
@Component({
  selector: 'app-nova-os',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, BuscaPlacaComponent],
  template: `
    <h2 mat-dialog-title>Nova OS</h2>
    <mat-dialog-content>
      @if (veiculoInicial) {
        <p class="escolhido" data-testid="veiculo-escolhido">
          <span class="placa">{{ veiculoInicial.placa }}</span> {{ veiculoInicial.marca }} {{ veiculoInicial.modelo }} · {{ veiculoInicial.dono.nome }}
        </p>
      } @else {
        <app-busca-placa (encontrado)="veiculo.set($event)" />
      }

      @if (veiculo(); as v) {
        <form [formGroup]="form" (ngSubmit)="abrir()" id="form-nova-os" class="dados-os" novalidate>
          <mat-form-field class="largo">
            <mat-label>Problema relatado pelo cliente</mat-label>
            <textarea matInput formControlName="problemaRelatado" rows="3" data-testid="problema"></textarea>
            @if (form.controls.problemaRelatado.hasError('required')) {
              <mat-error>Descreva o problema</mat-error>
            }
            @if (form.controls.problemaRelatado.hasError('api')) {
              <mat-error>{{ form.controls.problemaRelatado.getError('api') }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field>
            <mat-label>Quilometragem (opcional)</mat-label>
            <input matInput type="number" inputmode="numeric" formControlName="quilometragem" />
            @if (form.controls.quilometragem.invalid) {
              <mat-error>Quilometragem inválida</mat-error>
            }
          </mat-form-field>
        </form>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="acao-principal" type="submit" form="form-nova-os"
              [disabled]="!veiculo() || salvando()" data-testid="abrir-os">
        Abrir OS
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dados-os { display: grid; grid-template-columns: 1fr; margin-top: 16px; }
    .escolhido { margin: 4px 0 0; }
  `,
})
export class NovaOsComponent {
  private readonly api = inject(OrdensService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialogRef<NovaOsComponent, OrdemCriada>);

  /** Veículo já escolhido por quem abriu o diálogo (ex.: histórico do veículo); sem ele, busca pela placa. */
  readonly veiculoInicial = inject<{ veiculo?: VeiculoComDono } | null>(MAT_DIALOG_DATA, { optional: true })?.veiculo ?? null;
  readonly veiculo = signal<VeiculoComDono | null>(this.veiculoInicial);
  readonly salvando = signal(false);
  readonly form = inject(FormBuilder).group({
    problemaRelatado: ['', [Validators.required, Validators.maxLength(2000)]],
    quilometragem: [null as number | null, [Validators.min(0)]],
  });

  abrir(): void {
    const veiculo = this.veiculo();
    if (!veiculo) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const { problemaRelatado, quilometragem } = this.form.getRawValue();
    this.api
      .abrir({ veiculoId: veiculo.id, problemaRelatado: (problemaRelatado ?? '').trim(), quilometragem: quilometragem ?? null })
      .subscribe({
        next: (os) => {
          this.salvando.set(false);
          this.mensagens.sucesso(`OS #${os.numero} aberta.`);
          this.dialogo.close(os);
        },
        error: (erro) => {
          this.salvando.set(false);
          if (!aplicarErrosDaApi(this.form, erro)) {
            this.mensagens.erro(erro);
          }
        },
      });
  }
}
