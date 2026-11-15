import { Component, inject, input } from '@angular/core';
import { Expense } from './models';
import { FinanceStore } from './finance-store';
import { money, shortDate } from './finance-utils';
import { Icon } from './icon';

@Component({ selector: 'app-expense-table', imports: [Icon], template: `
  @if (expenses().length) {
    <table class="ct-table"><thead><tr><th>Descrição</th><th class="ct-category-col">Categoria</th><th class="ct-date-col">Data</th><th class="ct-money">Valor</th>@if (editable()) {<th><span class="ct-hidden-label">Ação</span></th>}</tr></thead><tbody>
    @for (expense of expenses(); track expense.id) {
      <tr [attr.data-expense-id]="expense.id"><td><span class="ct-expense-description"><span class="ct-expense-icon"><app-icon [name]="expense.recurrenceId ? 'repeat' : 'receipt'" /></span><span class="ct-description-copy">{{expense.description}}<span class="ct-description-meta">{{expense.recurrenceId ? 'Recorrente · ' : ''}}{{shortDate(expense.date)}} · <span class="ct-inline-category"><span class="ct-dot" [style.background]="expense.category.color"></span>{{expense.category.name}}</span></span></span></span></td><td class="ct-category-cell"><span class="ct-category-pill"><span class="ct-dot" [style.background]="expense.category.color"></span>{{expense.category.name}}</span></td><td class="ct-date-cell">{{shortDate(expense.date)}}</td><td class="ct-money">{{money(expense.amount)}}</td>@if (editable()) {<td><button type="button" class="ct-edit-button" [disabled]="store.disabled()" [attr.aria-label]="'Editar ' + expense.description" (click)="store.editExpense(expense)">Editar</button></td>}</tr>
    }</tbody></table>
  } @else { <p class="ct-empty">Nenhum gasto registrado neste mês.</p> }
` })
export class ExpenseTable {
  readonly expenses = input<Expense[]>([]); readonly editable = input(false);
  readonly store = inject(FinanceStore); readonly money = money; readonly shortDate = shortDate;
}
