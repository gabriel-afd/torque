import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { formatarDocumento, formatarTelefone } from '../shared/validadores';
import { Cliente } from './clientes.modelos';
import { ClientesService } from './clientes.service';
import { NovoClienteComponent } from './novo-cliente.component';
import { comCarregamento } from '../shared/carregamento';
import { ListaPaginada } from '../shared/lista-paginada';

@Component({
  selector: 'app-cliente-lista',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatPaginatorModule, MatProgressBarModule,
  ],
  templateUrl: './cliente-lista.component.html',
  styleUrl: './cliente-lista.component.scss',
})
export class ClienteListaComponent implements OnInit {
  /** Dado da rota /clientes/novo: abre o cadastro em diálogo sobre a lista. */
  readonly novo = input(false);

  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);

  readonly busca = new FormControl('', { nonNullable: true });
  readonly lista = new ListaPaginada<Cliente>();
  readonly carregando = signal(false);

  readonly documento = formatarDocumento;
  readonly telefone = formatarTelefone;

  ngOnInit(): void {
    this.carregar();
    if (this.novo()) {
      this.novoCliente();
    }
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.lista.recomecar();
        this.carregar();
      });
  }

  /** Abre o cadastro em diálogo; cadastrado, vai para o detalhe do cliente (onde se cadastram os veículos). */
  novoCliente(): void {
    this.dialogo
      .open(NovoClienteComponent, { width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((cliente?: Cliente) => {
        if (cliente) {
          this.router.navigate(['/clientes', cliente.id]);
        } else if (this.novo()) {
          this.router.navigate(['/clientes'], { replaceUrl: true });
        }
      });
  }

  mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
    this.carregar();
  }

  carregar(): void {
    this.api.listar(this.busca.value, this.lista.pagina(), this.lista.tamanho()).pipe(comCarregamento(this.carregando)).subscribe({
      next: (resposta) => this.lista.receber(resposta),
      error: (erro) => this.mensagens.erro(erro),
    });
  }
}
