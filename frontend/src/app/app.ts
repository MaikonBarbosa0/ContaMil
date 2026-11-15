import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { FinanceStore } from './finance-store';
import { Icon } from './icon';
import { ExpenseDialog } from './expense-dialog';

@Component({ selector: 'app-root', imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon, ExpenseDialog], templateUrl: './app.html' })
export class App {
  readonly store = inject(FinanceStore);
  readonly theme = signal<'light'|'dark'>(this.savedTheme());
  readonly pageName = signal('Visão geral');
  readonly navigation = [
    { path:'/resumo', label:'Visão geral', icon:'dashboard' as const },
    { path:'/gastos', label:'Gastos', icon:'receipt' as const },
    { path:'/salario', label:'Salário', icon:'wallet' as const },
    { path:'/categorias', label:'Categorias', icon:'palette' as const },
  ];
  constructor() {
    const router = inject(Router);
    router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(event => this.pageName.set(this.navigation.find(item => item.path === event.urlAfterRedirects.split('?')[0])?.label ?? 'Visão geral'));
    effect(() => { document.documentElement.style.colorScheme = this.theme(); try { localStorage.setItem('contamil-theme',this.theme()); } catch {} });
    this.store.load();
  }
  private savedTheme(): 'light'|'dark' { try { return localStorage.getItem('contamil-theme') === 'dark' ? 'dark' : 'light'; } catch { return 'light'; } }
}
