import { Component, effect, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FinanceStore } from './finance-store';
import { Icon } from './icon';
import { ExpenseTable } from './expense-table';
import { SpendingCharts } from './spending-charts';
import { cents, fullDate, isColor, money, monthName, validMonth } from './finance-utils';
import { Category, Recurrence } from './models';

@Component({ selector: 'app-summary-page', imports: [FormsModule, RouterLink, Icon, ExpenseTable, SpendingCharts], template: `
  <section class="ct-page" aria-label="Visão geral">
    <div class="ct-page-heading"><div><p class="ct-eyebrow">SUA VISÃO DO MÊS</p><h1>Seu dinheiro, com clareza.</h1><p class="ct-subtitle">Entenda seus gastos. Cuide do que importa.</p></div><label class="ct-period"><span>Mês de referência</span><input type="month" aria-label="Mês de referência do resumo" [ngModel]="store.month()" (ngModelChange)="store.selectMonth($event)" min="0001-01" max="9999-12"></label></div>
    <div class="ct-metrics">
      <section class="ct-metric"><span class="ct-metric-label"><span>Salário do mês</span><app-icon name="wallet" /></span><strong data-salary-value>{{money(store.summary()?.salary ?? 0)}}</strong><span class="ct-metric-caption">Seu salário informado</span></section>
      <section class="ct-metric"><span class="ct-metric-label"><span>Total de gastos</span><app-icon name="up" /></span><strong data-total-value>{{money(store.summary()?.totalExpenses ?? 0)}}</strong><span class="ct-metric-caption">{{store.expenses().length}} lançamentos no mês</span></section>
      <section class="ct-metric ct-balance"><span class="ct-metric-label"><span>Saldo do mês</span><app-icon name="scale" /></span><strong data-balance-value>{{money(store.summary()?.balance ?? 0)}}</strong><span class="ct-metric-caption">Salário menos gastos</span></section>
    </div>
    @if (!store.salaries().length && !store.loading()) {<div class="ct-info"><app-icon name="wallet" /><p>Comece informando seu salário em <a routerLink="/salario">Salário</a>. Depois, registre os gastos do seu mês.</p></div>}
    <app-spending-charts [summary]="store.summary()" />
    <section class="ct-panel ct-recent"><div class="ct-panel-heading"><div><h2>Últimos gastos</h2><p>As pequenas e grandes saídas do seu mês.</p></div><a class="ct-text-button" routerLink="/gastos">Ver todos <app-icon name="arrow" /></a></div><app-expense-table [expenses]="store.expenses().slice(0,3)" /></section>
    <div class="ct-summary-action"><span>O próximo gasto também conta.</span><button type="button" class="ct-primary" [disabled]="store.disabled()" (click)="store.openNew()"><app-icon name="plus" />Novo gasto</button></div>
  </section>
` })
export class SummaryPage { readonly store = inject(FinanceStore); readonly money = money; }

@Component({ selector: 'app-expenses-page', imports: [FormsModule, Icon, ExpenseTable], template: `
  <section class="ct-page" aria-label="Seus gastos">
    <div class="ct-page-heading"><div><p class="ct-eyebrow">UM REGISTRO DE CADA VEZ</p><h1>Seus gastos.</h1><p class="ct-subtitle">Tudo que saiu, reunido em um só lugar.</p></div><button type="button" class="ct-primary" [disabled]="store.disabled()" (click)="store.openNew()"><app-icon name="plus" />Novo gasto</button></div>
    <div class="ct-expense-toolbar"><label class="ct-period"><span>Mês de referência</span><input type="month" aria-label="Mês de referência dos gastos" [ngModel]="store.month()" (ngModelChange)="store.selectMonth($event)" min="0001-01" max="9999-12"></label><span>{{store.expenses().length}} gastos · {{money(store.summary()?.totalExpenses ?? 0)}}</span></div>
    <section class="ct-panel ct-expense-panel"><div class="ct-panel-heading"><div><h2>Lançamentos do mês</h2><p>Gastos avulsos e recorrentes.</p></div></div><app-expense-table [expenses]="store.expenses()" [editable]="true" /></section>
    <p class="ct-bottom-note"><app-icon name="repeat" />Editar um lançamento recorrente altera somente aquele gasto.</p>
    <section class="ct-panel ct-recurring-panel"><div class="ct-panel-heading"><div><h2>Despesas recorrentes</h2><p>Alterações na recorrência valem para os próximos lançamentos.</p></div><app-icon name="repeat" /></div>
      @if (store.recurrences().length) {<div class="ct-recurring-list">@for (item of store.recurrences(); track item.id) {
        <div class="ct-recurrence" [attr.data-recurrence-id]="item.id"><div class="ct-recurrence-copy"><strong>{{item.description}}</strong><span><span class="ct-dot" [style.background]="item.category.color"></span>{{item.category.name}} · {{money(item.amount)}} por mês</span><small>{{item.active ? 'Próximo lançamento: ' + fullDate(item.nextDate ?? '') : 'Cancelada · histórico preservado'}}</small></div>@if (item.active) {<div class="ct-row-actions"><button class="ct-text-button" type="button" [disabled]="store.disabled()" [attr.aria-label]="'Editar recorrência ' + item.description" (click)="store.editRecurrence(item)">Editar recorrência</button><button class="ct-delete" type="button" [disabled]="store.disabled()" [attr.aria-label]="'Cancelar recorrência ' + item.description" (click)="cancel(item)">Cancelar</button></div>}</div>
      }</div>} @else {<p class="ct-empty">Ao registrar um gasto, marque “Repetir todo mês” para criar uma despesa recorrente.</p>}
    </section>
  </section>
` })
export class ExpensesPage {
  readonly store = inject(FinanceStore); readonly money = money; readonly fullDate = fullDate;
  cancel(item: Recurrence): void {
    if (window.confirm(`Cancelar a recorrência “${item.description}”? Os lançamentos anteriores serão mantidos.`)) void this.store.commit(this.store.api.cancelRecurrence(item.id), 'Recorrência cancelada. O histórico foi preservado.');
  }
}

@Component({ selector: 'app-salary-page', imports: [FormsModule, Icon], template: `
  <section class="ct-page" aria-label="Seu salário"><div class="ct-page-heading"><div><p class="ct-eyebrow">O PONTO DE PARTIDA</p><h1>Seu salário.</h1><p class="ct-subtitle">Uma base simples para entender o seu mês.</p></div></div>
    <div class="ct-salary-layout"><section class="ct-panel ct-salary-form-panel"><div class="ct-panel-heading"><div><h2>Informar salário mensal</h2><p>Valores em reais.</p></div><app-icon name="wallet" /></div>
      <form #salaryForm="ngForm" (ngSubmit)="save(salaryForm)"><label class="ct-field"><span>Salário mensal (R$)</span><input name="amount" type="number" min="0" max="999999999999.99" step="0.01" inputmode="decimal" [(ngModel)]="amount" required></label><label class="ct-field"><span>A partir de qual mês?</span><input name="month" type="month" min="0001-01" max="9999-12" [(ngModel)]="month" required></label><div class="ct-info"><app-icon name="calendar" /><p>A mudança vale a partir do mês escolhido. Os meses anteriores mantêm o salário que você havia informado.</p></div>@if (formError) {<p class="ct-form-error" role="alert">{{formError}}</p>}<button type="submit" class="ct-primary" [disabled]="store.disabled()">{{store.saving() ? 'Salvando…' : 'Salvar salário'}}</button></form>
    </section><section class="ct-panel ct-history-panel"><div class="ct-panel-heading"><div><h2>Histórico de salário</h2><p>O que vale em cada período.</p></div></div>@for (salary of store.salaries(); track salary.month) {<div class="ct-history-item"><span>A partir de {{monthName(salary.month)}}</span><strong>{{money(salary.amount)}}</strong></div>} @empty {<p class="ct-empty">Nenhum salário informado.</p>}</section></div>
  </section>
` })
export class SalaryPage {
  readonly store = inject(FinanceStore); readonly monthName = monthName; readonly money = money;
  amount: number | string | null = null; month = this.store.month(); formError = '';
  constructor() { effect(() => { const summary = this.store.summary(); if (summary) { this.amount = summary.salary.toFixed(2); this.month = summary.month; } }); }
  save(form: NgForm): void {
    const amount = cents(this.amount);
    if (!form.valid || amount === null || !validMonth(this.month)) { this.formError = 'Informe um salário válido com até duas casas decimais e escolha o mês.'; form.form.markAllAsTouched(); return; }
    this.formError = '';
    void this.store.commit(this.store.api.salary(this.month, amount/100), `Salário salvo a partir de ${monthName(this.month)}.`);
  }
}

@Component({ selector: 'app-categories-page', imports: [FormsModule, Icon], template: `
  <section class="ct-page" aria-label="Suas categorias"><div class="ct-page-heading"><div><p class="ct-eyebrow">CADA CATEGORIA, UMA COR</p><h1>Suas categorias.</h1><p class="ct-subtitle">Os nomes são fixos. As cores ficam com o seu jeito.</p></div></div>
    <section class="ct-panel ct-colors-panel"><div class="ct-panel-heading"><div><h2>Cores das categorias</h2><p>A mesma cor acompanha a categoria nos gastos e nos gráficos.</p></div><app-icon name="palette" /></div><div class="ct-category-settings">
      @for (category of store.categories(); track category.id) {<div class="ct-color-row" [attr.data-category-row]="category.id"><span class="ct-color-name"><span class="ct-dot" [style.background]="category.color"></span><strong>{{category.name}}</strong></span><span class="ct-color-controls"><input type="color" class="ct-color-picker" [attr.aria-label]="'Cor de ' + category.name" [ngModel]="drafts[category.id] ?? category.color" (ngModelChange)="drafts[category.id]=$event" (change)="save(category)" [disabled]="store.disabled()"><input type="text" class="ct-color-hex" maxlength="7" pattern="#[0-9A-Fa-f]{6}" [attr.aria-label]="'Código hexadecimal da cor de ' + category.name" autocomplete="off" spellcheck="false" [ngModel]="drafts[category.id] ?? category.color" (ngModelChange)="drafts[category.id]=$event" (change)="save(category)" (keydown.enter)="save(category)" [disabled]="store.disabled()"></span>@if (invalidId === category.id) {<small class="ct-color-error" role="alert">Use # seguido de seis dígitos hexadecimais.</small>}</div>}
    </div></section><p class="ct-bottom-note"><app-icon name="check" />As mudanças de cor aparecem em todos os meses e nos dois temas.</p>
  </section>
` })
export class CategoriesPage {
  readonly store = inject(FinanceStore); drafts: Record<number,string> = {}; invalidId: number | null = null;
  constructor() { effect(() => { this.drafts = Object.fromEntries(this.store.categories().map(category => [category.id,category.color])); }); }
  async save(category: Category): Promise<void> {
    const color = (this.drafts[category.id] ?? category.color).toUpperCase();
    if (!isColor(color)) { this.invalidId = category.id; return; }
    this.invalidId = null;
    if (color === category.color.toUpperCase() || this.store.disabled()) return;
    const saved = await this.store.commit(this.store.api.color(category.id,color), `Cor de ${category.name} atualizada.`);
    if (!saved) this.drafts[category.id] = category.color;
  }
}
