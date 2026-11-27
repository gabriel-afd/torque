import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { Perfil, ROTULO_PERFIL } from '../core/modelos';

export interface ItemMenu {
  rotulo: string;
  rota: string;
  /** Arquivo em public/icones (sem extensão). */
  icone: string;
  /** Vazio = todos os perfis. */
  perfis: Perfil[];
}

export const ITENS_MENU: ItemMenu[] = [
  { rotulo: 'Quadro de OS', rota: '/quadro', icone: 'quadro', perfis: [] },
  { rotulo: 'Clientes', rota: '/clientes', icone: 'clientes', perfis: ['ATENDENTE', 'ADMINISTRADOR'] },
  { rotulo: 'Veículos', rota: '/veiculos', icone: 'veiculos', perfis: [] },
  { rotulo: 'Catálogo', rota: '/catalogo', icone: 'catalogo', perfis: ['ADMINISTRADOR'] },
  { rotulo: 'Painel', rota: '/admin/painel', icone: 'painel', perfis: ['ADMINISTRADOR'] },
  { rotulo: 'Usuários', rota: '/admin/usuarios', icone: 'usuarios', perfis: ['ADMINISTRADOR'] },
];

/** Seção da trilha "Oficina / …" a partir da URL (o quadro e as OS ficam em "Ordens de serviço"). */
export function secaoDaUrl(url: string): string {
  const caminho = url.split(/[?#]/)[0];
  if (caminho.startsWith('/quadro') || caminho.startsWith('/ordens')) {
    return 'Ordens de serviço';
  }
  return ITENS_MENU.find((item) => caminho.startsWith(item.rota))?.rotulo ?? '';
}

/** Menu lateral por perfil (vira gaveta no celular), trilha da página, usuário logado e área das páginas. */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule, MatSidenavModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly celular = toSignal(
    inject(BreakpointObserver).observe('(max-width: 900px)').pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );
  readonly secao = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map(() => secaoDaUrl(this.router.url)),
      startWith(secaoDaUrl(this.router.url)),
    ),
    { initialValue: '' },
  );

  readonly usuario = this.auth.usuario;
  readonly itens = computed(() => {
    const perfil = this.auth.perfil();
    return ITENS_MENU.filter((item) => item.perfis.length === 0 || (perfil !== null && item.perfis.includes(perfil)));
  });
  readonly rotuloPerfil = computed(() => {
    const perfil = this.auth.perfil();
    return perfil ? ROTULO_PERFIL[perfil] : '';
  });
  readonly iniciais = computed(() =>
    (this.usuario()?.nome ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0].toUpperCase())
      .join(''),
  );

  icone(item: ItemMenu): string {
    return `url('/icones/${item.icone}.svg')`;
  }

  sair(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
