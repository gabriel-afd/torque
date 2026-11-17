import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Observable, filter, forkJoin } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { MensagemService } from '../core/mensagem.service';
import { confirmar } from '../shared/confirmacao.component';
import { formatarTelefone } from '../shared/validadores';
import {
  Acao, Rascunho, acoesDisponiveis, alterado, avisoDeDesconto, edicaoCriaNovaVersao, rascunhoDe, requisicaoDe, resumir,
} from './orcamento';
import { OrcamentoItensComponent } from './orcamento-itens.component';
import { HistoricoStatus, OrdemServico, ROTULO_STATUS, StatusOS } from './ordens.modelos';
import { OrdensService } from './ordens.service';
import { quando } from './quadro';
import { comCarregamento } from '../shared/carregamento';

const VAZIO: Rascunho = { servicos: [], pecas: [], desconto: 0 };

/** Tela da OS com orçamento, ações de status e histórico (HU04–HU07), conforme docs/prototipos/orcamento.html. */
@Component({
  selector: 'app-ordem-detalhe',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, CurrencyPipe, DecimalPipe, MatButtonModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatProgressBarModule, MatSelectModule, OrcamentoItensComponent,
  ],
  templateUrl: './ordem-detalhe.component.html',
  styleUrl: './ordem-detalhe.component.scss',
})
export class OrdemDetalheComponent implements OnInit {
  private readonly api = inject(OrdensService);
  private readonly auth = inject(AuthService);
  private readonly mensagens = inject(MensagemService);
  private readonly dialogo = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  /** Parâmetro :id da rota (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly os = signal<OrdemServico | null>(null);
  readonly rascunho = signal<Rascunho>(VAZIO);
  readonly carregando = signal(false);
  readonly processando = signal(false);
  /** RN07: o usuário confirmou que vai alterar um orçamento aprovado (nova versão ao salvar). */
  readonly editandoAprovado = signal(false);
  readonly mecanicos = signal<{ id: number; nome: string }[]>([]);

  readonly desconto = new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] });
  readonly mecanicoEscolhido = new FormControl<number | null>(null);

  readonly rotuloStatus = ROTULO_STATUS;
  readonly resumo = computed(() => resumir(this.rascunho()));
  readonly perfil = this.auth.perfil;
  readonly acoes = computed<Set<Acao>>(() => {
    const os = this.os();
    if (!os) {
      return new Set();
    }
    const atribuida = os.mecanico !== null && os.mecanico.id === this.auth.usuarioId();
    return acoesDisponiveis(os.status, { perfil: this.auth.perfil(), atribuidaAoUsuario: atribuida });
  });
  readonly criaNovaVersao = computed(() => {
    const os = this.os();
    return os !== null && edicaoCriaNovaVersao(os);
  });
  /** Itens e desconto editáveis: perfil/status permitem e, com orçamento aprovado, depois de confirmar a RN07. */
  readonly editavel = computed(() => this.tem('editarOrcamento') && (!this.criaNovaVersao() || this.editandoAprovado()));
  readonly alterado = computed(() => {
    const os = this.os();
    return os !== null && alterado(this.rascunho(), os);
  });
  readonly avisoDesconto = computed(() => avisoDeDesconto(this.resumo(), this.auth.perfil()));
  /** Histórico do mais recente para o mais antigo. */
  readonly historico = computed(() => [...(this.os()?.historico ?? [])].reverse());

  ngOnInit(): void {
    this.carregar();
    if (this.auth.perfil() === 'ADMINISTRADOR') {
      this.api.mecanicosAtivos().subscribe({ next: (lista) => this.mecanicos.set(lista), error: () => this.mecanicos.set([]) });
    }
    this.desconto.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((valor) => this.rascunho.update((r) => ({ ...r, desconto: Number(valor) || 0 })));
  }

  carregar(): void {
    this.api.detalhar(Number(this.id())).pipe(comCarregamento(this.carregando)).subscribe({
      next: (os) => this.mostrar(os),
      error: (erro) => this.mensagens.erro(erro),
    });
  }

  tem(acao: Acao): boolean {
    return this.acoes().has(acao);
  }

  // Cabeçalho ---------------------------------------------------------------------------------

  veiculo(os: OrdemServico): string {
    const v = os.veiculo;
    return [`${v.marca} ${v.modelo}${v.ano ? ' ' + v.ano : ''}`, v.cor].filter(Boolean).join(' · ');
  }

  cliente(os: OrdemServico): string {
    return os.cliente.telefone ? `${os.cliente.nome} · ${formatarTelefone(os.cliente.telefone)}` : os.cliente.nome;
  }

  quando(data: string): string {
    return quando(new Date(data), new Date());
  }

  rotuloHistorico(h: HistoricoStatus): string {
    return ROTULO_STATUS[h.statusNovo];
  }

  // Orçamento ---------------------------------------------------------------------------------

  /** RN07: avisa antes de liberar a edição de um orçamento já aprovado. */
  alterarOrcamentoAprovado(): void {
    const os = this.os();
    if (!os) {
      return;
    }
    confirmar(this.dialogo, {
      titulo: 'Alterar orçamento aprovado',
      mensagem: `O orçamento versão ${os.orcamento.versao} já foi aprovado e não pode ser modificado. Ao salvar, será criada a ` +
        `versão ${os.orcamento.versao + 1} e a OS volta para "Aguardando aprovação" do cliente.`,
      confirmar: 'Criar nova versão',
    })
      .pipe(filter(Boolean))
      .subscribe(() => this.editandoAprovado.set(true));
  }

  salvarOrcamento(): void {
    const os = this.os();
    if (!os) {
      return;
    }
    const novaVersao = this.criaNovaVersao();
    this.executar(
      this.api.alterarOrcamento(os.id, os.orcamento.versao, requisicaoDe(this.rascunho(), os.version)),
      novaVersao ? 'Nova versão do orçamento criada. A OS voltou para aguardando aprovação.' : 'Orçamento salvo.',
    );
  }

  descartar(): void {
    const os = this.os();
    if (os) {
      this.mostrar(os);
    }
  }

  // Ações -------------------------------------------------------------------------------------

  compartilhar(): void {
    const os = this.os();
    if (!os) {
      return;
    }
    forkJoin({ pdf: this.api.pdf(os.id, os.orcamento.versao), link: this.api.compartilhamento(os.id) })
      .pipe(comCarregamento(this.processando))
      .subscribe({
      next: ({ pdf, link }) => {
        this.salvarArquivo(pdf, `orcamento-os-${os.numero}-v${os.orcamento.versao}.pdf`);
        if (!this.abrirLink(link.linkWhatsapp)) {
          this.mensagens.aviso('PDF baixado. O navegador bloqueou a janela do WhatsApp: permita pop-ups e tente de novo.');
        }
      },
      error: (erro) => this.mensagens.erro(erro),
    });
  }

  enviarParaAprovacao(): void {
    this.mudarStatus('AGUARDANDO_APROVACAO', 'Orçamento enviado para aprovação do cliente.');
  }

  aprovar(): void {
    const os = this.os();
    if (os) {
      this.executar(this.api.aprovar(os.id, os.orcamento.versao), 'Aprovação do cliente registrada.');
    }
  }

  recusar(): void {
    const os = this.os();
    if (!os) {
      return;
    }
    confirmar(this.dialogo, {
      titulo: 'Registrar recusa',
      mensagem: `O cliente recusou o orçamento? A OS #${os.numero} será encerrada como recusada e não poderá ser reaberta.`,
      confirmar: 'Registrar recusa',
      perigo: true,
    })
      .pipe(filter(Boolean))
      .subscribe(() => this.executar(this.api.recusar(os.id, os.orcamento.versao), 'Recusa registrada.'));
  }

  iniciar(): void {
    this.mudarStatus('EM_EXECUCAO', 'Serviço em execução.');
  }

  aguardarPeca(): void {
    this.mudarStatus('AGUARDANDO_PECA', 'OS aguardando peça.');
  }

  concluir(): void {
    this.mudarStatus('CONCLUIDA', 'Serviço concluído.');
  }

  entregar(): void {
    this.mudarStatus('ENTREGUE', 'Veículo entregue.');
  }

  cancelar(): void {
    const os = this.os();
    if (!os) {
      return;
    }
    confirmar(this.dialogo, {
      titulo: 'Cancelar OS',
      mensagem: `Cancelar a OS #${os.numero} (${os.veiculo.placa})? Uma OS cancelada não pode ser reaberta.`,
      confirmar: 'Cancelar OS',
      perigo: true,
    })
      .pipe(filter(Boolean))
      .subscribe(() => this.mudarStatus('CANCELADA', 'OS cancelada.'));
  }

  atribuirMecanico(): void {
    const os = this.os();
    const mecanicoId = this.mecanicoEscolhido.value;
    if (os && mecanicoId !== null) {
      this.executar(this.api.atribuirMecanico(os.id, mecanicoId, os.version), 'Mecânico atribuído.');
    }
  }

  // Apoio -------------------------------------------------------------------------------------

  /** Baixa o arquivo pelo navegador (separado para os testes não abrirem downloads). */
  salvarArquivo(conteudo: Blob, nome: string): void {
    const url = URL.createObjectURL(conteudo);
    const link = document.createElement('a');
    link.href = url;
    link.download = nome;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  /** Abre o link em nova aba; false se o navegador bloquear a janela. */
  abrirLink(url: string): boolean {
    const janela = window.open(url, '_blank');
    if (janela) {
      janela.opener = null;
    }
    return janela !== null;
  }

  private mudarStatus(status: StatusOS, sucesso: string): void {
    const os = this.os();
    if (os) {
      this.executar(this.api.alterarStatus(os.id, status, os.version), sucesso);
    }
  }

  /** Envia a ação à API: sucesso atualiza a tela; erro (422, 409, 403…) mostra a mensagem da API. */
  private executar(acao: Observable<OrdemServico>, sucesso: string): void {
    acao.pipe(comCarregamento(this.processando)).subscribe({
      next: (os) => {
        this.mostrar(os);
        this.mensagens.sucesso(sucesso);
      },
      error: (erro) => this.mensagens.erro(erro),
    });
  }

  private mostrar(os: OrdemServico): void {
    this.os.set(os);
    this.rascunho.set(rascunhoDe(os));
    this.desconto.setValue(os.orcamento.desconto, { emitEvent: false });
    this.editandoAprovado.set(false);
    this.mecanicoEscolhido.setValue(os.mecanico?.id ?? null);
  }
}
