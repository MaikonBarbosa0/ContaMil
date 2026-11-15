import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom, forkJoin, Observable, Subscription } from 'rxjs';
import { apiError, FinanceApi } from './finance-api';
import { Category, Editor, Expense, Recurrence, Salary, Summary } from './models';
import { todayISO, validMonth } from './finance-utils';

@Injectable({ providedIn: 'root' })
export class FinanceStore {
  readonly api = inject(FinanceApi);
  readonly month = signal(todayISO().slice(0, 7));
  readonly categories = signal<Category[]>([]);
  readonly summary = signal<Summary | null>(null);
  readonly expenses = signal<Expense[]>([]);
  readonly salaries = signal<Salary[]>([]);
  readonly recurrences = signal<Recurrence[]>([]);
  readonly editor = signal<Editor | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly disabled = computed(() => this.loading() || this.saving());
  readonly error = signal('');
  readonly message = signal('');
  private request?: Subscription;

  load(): void {
    this.request?.unsubscribe();
    const month = this.month();
    if (this.summary()?.month !== month) { this.summary.set(null); this.expenses.set([]); }
    this.loading.set(true);
    this.error.set('');
    this.request = forkJoin({
      categories: this.api.categories(), summary: this.api.summary(month), expenses: this.api.expenses(month),
      salaries: this.api.salaries(), recurrences: this.api.recurrences(),
    }).subscribe({
      next: data => {
        this.categories.set(data.categories); this.summary.set(data.summary); this.expenses.set(data.expenses);
        this.salaries.set(data.salaries); this.recurrences.set(data.recurrences); this.loading.set(false);
      },
      error: error => { this.error.set(apiError(error)); this.loading.set(false); },
    });
  }
  selectMonth(month: string): void {
    if (validMonth(month) && month !== this.month()) { this.month.set(month); this.message.set(''); this.load(); }
  }
  async commit(request: Observable<unknown>, message: string, onSaved?: () => void): Promise<boolean> {
    if (this.disabled()) return false;
    this.saving.set(true); this.error.set(''); this.message.set('');
    try {
      await firstValueFrom(request);
      onSaved?.(); this.message.set(message); this.load();
      return true;
    } catch (error) {
      this.error.set(apiError(error));
      return false;
    } finally { this.saving.set(false); }
  }
  openNew(): void { this.error.set(''); this.editor.set({ kind: 'new' }); }
  editExpense(expense: Expense): void { this.error.set(''); this.editor.set({ kind: 'expense', expense }); }
  editRecurrence(recurrence: Recurrence): void { this.error.set(''); this.editor.set({ kind: 'recurrence', recurrence }); }
}
