import { Component, ElementRef, computed, effect, inject, untracked, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { FinanceStore } from './finance-store';
import { cents, todayISO, validDate } from './finance-utils';
import { Icon } from './icon';

@Component({ selector: 'app-expense-dialog', imports: [FormsModule, Icon], host: { style: 'display:contents' }, template: `
  <dialog class="ct-dialog" #dialog aria-labelledby="expense-title" (cancel)="cancel($event)" (close)="closed()">
    <div class="ct-dialog-heading"><div><p class="ct-eyebrow">SEU DIA A DIA</p><h2 id="expense-title">{{title()}}</h2></div><button type="button" class="ct-icon-button" aria-label="Fechar formulário" [disabled]="store.saving()" (click)="close()"><app-icon name="close" /></button></div>
    <form #expenseForm="ngForm" (ngSubmit)="save(expenseForm)">
      <label class="ct-field"><span>Descrição</span><input name="description" type="text" maxlength="180" placeholder="Ex.: compra no mercado" [(ngModel)]="description" required autofocus></label>
      <div class="ct-form-row"><label class="ct-field"><span>Valor (R$)</span><input name="amount" type="number" min="0.01" max="999999999999.99" step="0.01" inputmode="decimal" placeholder="0,00" [(ngModel)]="amount" required></label><label class="ct-field"><span>{{isSeries() ? 'Data inicial' : 'Data'}}</span><input name="date" type="date" min="0001-01-01" max="9999-12-31" [(ngModel)]="date" [readOnly]="isSeries()" required></label></div>
      <label class="ct-field"><span>Categoria</span><span class="ct-category-control"><span class="ct-selected-color" [style.background]="selectedColor()" aria-hidden="true"></span><select name="categoryId" [(ngModel)]="categoryId" required><option [ngValue]="null" disabled>Selecione uma categoria</option>@for (category of store.categories(); track category.id) {<option [ngValue]="category.id">{{category.name}}</option>}</select></span><small>Escolha uma das categorias prontas.</small></label>
      @if (store.editor()?.kind === 'new') {<label class="ct-checkbox"><input type="checkbox" name="recurring" [(ngModel)]="recurring"><span><strong>Repetir todo mês</strong><small>Uma despesa mensal, como aluguel ou internet.</small></span></label>}
      @if (recurring || isSeries()) {<div class="ct-info"><app-icon name="repeat" /><p>{{isSeries() ? 'A data inicial mantém o dia da recorrência. As alterações valem somente para os próximos lançamentos.' : 'Lançamentos automáticos até a data atual. Quando o dia não existir no mês, será usado o último dia.'}}</p></div>}
      @if (individualRecurring()) {<div class="ct-info"><app-icon name="repeat" /><p>Você está alterando somente este lançamento. Para mudar os próximos, edite a recorrência na tela de gastos.</p></div>}
      @if (formError || store.error()) {<p class="ct-form-error" role="alert">{{formError || store.error()}}</p>}
      <div class="ct-dialog-actions">@if (store.editor()?.kind === 'expense') {<button type="button" class="ct-delete" [disabled]="store.disabled()" (click)="remove()">Excluir gasto</button>}<button type="button" class="ct-secondary" [disabled]="store.saving()" (click)="close()">Cancelar</button><button type="submit" class="ct-primary" [disabled]="store.disabled()">{{store.saving() ? 'Salvando…' : isSeries() ? 'Salvar recorrência' : 'Salvar gasto'}}</button></div>
    </form>
  </dialog>
` })
export class ExpenseDialog {
  readonly store = inject(FinanceStore);
  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');
  description = ''; amount: number | string | null = null; date = todayISO(); categoryId: number | null = null;
  recurring = false; formError = '';
  readonly isSeries = computed(() => this.store.editor()?.kind === 'recurrence');
  readonly title = computed(() => this.isSeries() ? 'Editar recorrência' : this.store.editor()?.kind === 'expense' ? 'Editar gasto' : 'Novo gasto');
  readonly individualRecurring = computed(() => { const editor = this.store.editor(); return editor?.kind === 'expense' && !!editor.expense.recurrenceId; });
  selectedColor(): string { return this.store.categories().find(category => category.id === Number(this.categoryId))?.color ?? 'var(--ct-line)'; }
  constructor() {
    effect(() => {
      const element = this.dialog()?.nativeElement; const editor = this.store.editor();
      if (!element) return;
      if (editor) {
        untracked(() => {
          const item = editor.kind === 'expense' ? editor.expense : editor.kind === 'recurrence' ? editor.recurrence : null;
          this.description = item?.description ?? ''; this.amount = item ? item.amount.toFixed(2) : null;
          this.date = editor.kind === 'expense' ? editor.expense.date : editor.kind === 'recurrence' ? editor.recurrence.startDate : todayISO();
          this.categoryId = item?.category.id ?? null; this.recurring = false; this.formError = '';
        });
        if (!element.open) element.showModal();
      } else if (element.open) element.close();
    });
  }
  cancel(event: Event): void { if (this.store.saving()) event.preventDefault(); else this.close(); }
  close(): void { if (!this.store.saving()) this.store.editor.set(null); }
  closed(): void { this.store.editor.set(null); }
  save(form: NgForm): void {
    const editor = this.store.editor(); const amount = cents(this.amount); const categoryId = Number(this.categoryId);
    if (!editor || !form.valid || !this.description.trim() || amount === null || amount <= 0 || !validDate(this.date) || !this.store.categories().some(category => category.id === categoryId)) {
      this.formError = 'Preencha descrição, valor com até duas casas decimais, data e uma categoria da lista.'; form.form.markAllAsTouched(); return;
    }
    this.formError = '';
    const common = { description: this.description.trim(), amount: amount/100, categoryId };
    const request = editor.kind === 'recurrence' ? this.store.api.updateRecurrence(editor.recurrence.id,common)
      : editor.kind === 'expense' ? this.store.api.updateExpense(editor.expense.id,{...common,date:this.date})
      : this.recurring ? this.store.api.createRecurrence({...common,startDate:this.date}) : this.store.api.createExpense({...common,date:this.date});
    const message = editor.kind === 'recurrence' ? 'Recorrência atualizada. Os lançamentos anteriores foram preservados.' : this.recurring ? 'Despesa recorrente criada.' : 'Gasto salvo.';
    void this.store.commit(request,message,() => { if (editor.kind !== 'recurrence') this.store.month.set(this.date.slice(0,7)); this.store.editor.set(null); });
  }
  remove(): void {
    const editor = this.store.editor();
    if (editor?.kind === 'expense' && window.confirm(`Excluir o gasto “${editor.expense.description}”?`)) void this.store.commit(this.store.api.deleteExpense(editor.expense.id),'Gasto excluído.',() => this.store.editor.set(null));
  }
}
