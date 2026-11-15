import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Perfil, ROTULO_PERFIL } from '../core/modelos';

export interface ItemMenu {
  rotulo: string;
  rota: string;
  /** Vazio = todos os perfis. */
  perfis: Perfil[];
}

export const ITENS_MENU: ItemMenu[] = [
  { rotulo: 'Quadro de OS', rota: '/quadro', perfis: [] },
  { rotulo: 'Clientes', rota: '/clientes', perfis: ['ATENDENTE', 'ADMINISTRADOR'] },
  { rotulo: 'Veículos', rota: '/veiculos', perfis: [] },
  { rotulo: 'Catálogo', rota: '/catalogo', perfis: ['ADMINISTRADOR'] },
  { rotulo: 'Painel', rota: '/admin/painel', perfis: ['ADMINISTRADOR'] },
  { rotulo: 'Usuários', rota: '/admin/usuarios', perfis: ['ADMINISTRADOR'] },
];

/** Barra superior (menu por perfil, usuário logado, sair) e área das páginas. */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

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

  sair(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
