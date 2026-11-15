import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MensagemService } from '../core/mensagem.service';
import { formatarDocumento, formatarTelefone } from '../shared/validadores';
import { Cliente } from './clientes.modelos';
import { ClientesService } from './clientes.service';

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
  private readonly api = inject(ClientesService);
  private readonly mensagens = inject(MensagemService);
  private readonly destroyRef = inject(DestroyRef);

  readonly busca = new FormControl('', { nonNullable: true });
  readonly clientes = signal<Cliente[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly tamanho = signal(20);
  readonly carregando = signal(false);

  readonly documento = formatarDocumento;
  readonly telefone = formatarTelefone;

  ngOnInit(): void {
    this.carregar();
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pagina.set(0);
        this.carregar();
      });
  }

  mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.api.listar(this.busca.value, this.pagina(), this.tamanho()).subscribe({
      next: (resposta) => {
        this.clientes.set(resposta.content);
        this.total.set(resposta.page.totalElements);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.mensagens.erro(erro);
      },
    });
  }
}
