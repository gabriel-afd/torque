import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MascaraDirective } from '../shared/mascara.directive';
import type { FormVeiculo } from './veiculo-form.component';

/** Campos do veículo, reaproveitados no diálogo de veículo e no cadastro rápido pela placa. */
@Component({
  selector: 'app-veiculo-campos',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MascaraDirective],
  template: `
    <div class="campos" [formGroup]="form()">
      <mat-form-field>
        <mat-label>Placa</mat-label>
        <input matInput formControlName="placa" appMascara="placa" autocapitalize="characters" data-testid="placa" />
        <mat-hint>ABC1234 ou ABC1D23</mat-hint>
        @if (form().controls.placa.hasError('required')) {
          <mat-error>Informe a placa</mat-error>
        } @else if (form().controls.placa.hasError('placa')) {
          <mat-error data-testid="erro-placa">Placa inválida (use ABC1234 ou ABC1D23)</mat-error>
        } @else if (form().controls.placa.hasError('api')) {
          <mat-error data-testid="erro-placa">{{ form().controls.placa.getError('api') }}</mat-error>
        }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Marca</mat-label>
        <input matInput formControlName="marca" />
        @if (form().controls.marca.hasError('required')) { <mat-error>Informe a marca</mat-error> }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Modelo</mat-label>
        <input matInput formControlName="modelo" />
        @if (form().controls.modelo.hasError('required')) { <mat-error>Informe o modelo</mat-error> }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Ano</mat-label>
        <input matInput formControlName="ano" type="number" inputmode="numeric" />
        @if (form().controls.ano.invalid) { <mat-error>Ano inválido</mat-error> }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Cor</mat-label>
        <input matInput formControlName="cor" />
      </mat-form-field>
    </div>
  `,
  styles: `
    .campos { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); column-gap: 12px; padding-top: 8px; }
  `,
})
export class VeiculoCamposComponent {
  readonly form = input.required<FormVeiculo>();
}
