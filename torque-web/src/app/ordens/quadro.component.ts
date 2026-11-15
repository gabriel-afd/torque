import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { NovaOsComponent } from './nova-os.component';
import { COLUNAS_QUADRO, OrdemCriada, OrdemResumo } from './ordens.modelos';
import { OrdensService } from './ordens.service';
import { agruparPorStatus, emAlerta, infoDoCartao, situacaoDoCartao } from './quadro';

interface Mecanico {
  id: number;
  nome: string;
}

/** Quadro de OS por status (HU08), conforme docs/prototipos/quadro.html. */
@Component({
  selector: 'app-quadro',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './quadro.component.html',
  styleUrl: './quadro.component.scss',
})
export class QuadroComponent implements OnInit {
  private readonly api = inject(OrdensService);
  private readonly auth = inject(AuthService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly colunas = COLUNAS_QUADRO;
  readonly busca = new FormControl('', { nonNullable: true });
  readonly mecanico = new FormControl<number | null>(null);

  readonly ordens = signal<OrdemResumo[]>([]);
  readonly totalNaApi = signal(0);
  readonly carregando = signal(false);
  readonly agora = signal(new Date());
  /** Mecânicos vistos nas OS carregadas (acumulados para o filtro continuar completo após filtrar). */
  readonly mecanicos = signal<Mecanico[]>([]);

  readonly grupos = computed(() => agruparPorStatus(this.ordens()));
  readonly ehMecanico = computed(() => this.auth.perfil() === 'MECANICO');
  readonly podeAbrirOs = computed(() => this.auth.temPerfil('ATENDENTE', 'ADMINISTRADOR'));

  readonly emAlerta = (os: OrdemResumo) => emAlerta(os, this.agora());
  readonly info = (os: OrdemResumo) => infoDoCartao(os, this.agora());
  readonly situacao = (os: OrdemResumo) => situacaoDoCartao(os, this.agora());

  ngOnInit(): void {
    this.carregar();
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregar());
    this.mecanico.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.carregar());
  }

  carregar(): void {
    this.carregando.set(true);
    this.api
      .quadro({
        status: this.colunas.map((c) => c.status),
        // MECANICO: a API ignora o filtro e devolve só as dele; não precisa enviar
        mecanico: this.ehMecanico() ? null : this.mecanico.value,
        busca: this.busca.value,
      })
      .subscribe({
        next: (pagina) => {
          this.agora.set(new Date());
          this.ordens.set(pagina.content);
          this.totalNaApi.set(pagina.page.totalElements);
          this.lembrarMecanicos(pagina.content);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.carregando.set(false);
          this.mensagens.erro(erro);
        },
      });
  }

  abrir(os: OrdemResumo): void {
    this.router.navigate(['/ordens', os.id]);
  }

  novaOs(): void {
    this.dialogo
      .open(NovaOsComponent, { width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((criada?: OrdemCriada) => {
        if (criada) {
          this.router.navigate(['/ordens', criada.id]);
        }
      });
  }

  quantidade(status: string): number {
    return this.grupos().get(status as OrdemResumo['status'])?.length ?? 0;
  }

  private lembrarMecanicos(ordens: OrdemResumo[]): void {
    const conhecidos = new Map(this.mecanicos().map((m) => [m.id, m]));
    ordens.forEach((os) => {
      if (os.mecanicoId !== null && os.mecanico) {
        conhecidos.set(os.mecanicoId, { id: os.mecanicoId, nome: os.mecanico });
      }
    });
    this.mecanicos.set([...conhecidos.values()].sort((a, b) => a.nome.localeCompare(b.nome)));
  }
}
