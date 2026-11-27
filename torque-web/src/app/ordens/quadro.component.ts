import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
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
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { NovaOsComponent } from './nova-os.component';
import { COLUNAS_QUADRO, OrdemCriada, OrdemResumo, StatusOS } from './ordens.modelos';
import { OrdensService } from './ordens.service';
import {
  MovimentoNoQuadro, agruparPorStatus, doisDigitos, emAlerta, movimentoNoQuadro, resumoDoQuadro, tempoDoCartao, valorDoCartao,
} from './quadro';
import { comCarregamento } from '../shared/carregamento';

interface Mecanico {
  id: number;
  nome: string;
}

/**
 * Quadro de OS por status (HU08), conforme docs/prototipos/quadro.html. Os cartões podem ser arrastados entre
 * colunas quando a tabela de transições permite ({@link movimentoNoQuadro}).
 */
@Component({
  selector: 'app-quadro',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule, CdkDropListGroup, CdkDropList, CdkDrag,
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
  readonly valor = valorDoCartao;
  readonly tempo = (os: OrdemResumo) => tempoDoCartao(os, this.agora());
  readonly resumo = computed(() => resumoDoQuadro(this.ordens()));
  readonly dois = doisDigitos;
  readonly primeiroNome = (nome: string) => nome.split(' ')[0];

  /** No toque, o arraste só começa após segurar o cartão (o toque rápido continua rolando o quadro). */
  readonly atrasoDoArraste = { touch: 250, mouse: 0 };
  /** Quando o último arraste terminou: o clique que o navegador dispara ao soltar não deve abrir a OS. */
  private fimDoUltimoArraste = 0;

  ngOnInit(): void {
    this.carregar();
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregar());
    this.mecanico.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.carregar());
  }

  carregar(): void {
    this.api
      .quadro({
        status: this.colunas.map((c) => c.status),
        // MECANICO: a API ignora o filtro e devolve só as dele; não precisa enviar
        mecanico: this.ehMecanico() ? null : this.mecanico.value,
        busca: this.busca.value,
      })
      .pipe(comCarregamento(this.carregando)).subscribe({
        next: (pagina) => {
          this.agora.set(new Date());
          this.ordens.set(pagina.content);
          this.totalNaApi.set(pagina.page.totalElements);
          this.lembrarMecanicos(pagina.content);
        },
        error: (erro) => this.mensagens.erro(erro),
      });
  }

  abrir(os: OrdemResumo): void {
    if (Date.now() - this.fimDoUltimoArraste < 400) {
      return;
    }
    this.router.navigate(['/ordens', os.id]);
  }

  fimDoArraste(): void {
    this.fimDoUltimoArraste = Date.now();
  }

  /**
   * Cartão solto em outra coluna: movimento proibido só mostra o motivo; permitido move o cartão na hora e
   * executa a ação na API (com a versão atual da OS). Se a API recusar, o cartão volta e a mensagem dela aparece.
   */
  soltar(evento: CdkDragDrop<StatusOS, StatusOS, OrdemResumo>): void {
    const os = evento.item.data;
    const movimento = movimentoNoQuadro(evento.previousContainer.data, evento.container.data, {
      perfil: this.auth.perfil(),
      atribuidaAoUsuario: os.mecanicoId !== null && os.mecanicoId === this.auth.usuarioId(),
    });
    if (!movimento) {
      return;
    }
    if (!movimento.permitido) {
      this.mensagens.aviso(movimento.motivo);
      return;
    }
    this.moverCartao(os.id, movimento.destino);
    this.executar(os, movimento);
  }

  private executar(os: OrdemResumo, movimento: Extract<MovimentoNoQuadro, { permitido: true }>): void {
    this.api
      .detalhar(os.id)
      .pipe(
        switchMap((atual) =>
          movimento.acao === 'aprovar'
            ? this.api.aprovar(atual.id, atual.orcamento.versao)
            : this.api.alterarStatus(atual.id, movimento.destino, atual.version),
        ),
      )
      .subscribe({
        next: () => {
          this.mensagens.sucesso(`OS #${os.numero}: ${movimento.sucesso}`);
          this.carregar();
        },
        error: (erro) => {
          this.moverCartao(os.id, os.status);
          this.mensagens.erro(erro);
          this.carregar();
        },
      });
  }

  private moverCartao(id: number, status: StatusOS): void {
    this.ordens.update((lista) => lista.map((o) => (o.id === id ? { ...o, status } : o)));
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
