import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Category, Expense, ExpenseInput, Recurrence, RecurrenceInput, Salary, Summary } from './models';

@Injectable({ providedIn: 'root' })
export class FinanceApi {
  private readonly http = inject(HttpClient);
  categories = () => this.http.get<Category[]>('/api/categories');
  summary = (month: string) => this.http.get<Summary>('/api/summary', { params: { month } });
  expenses = (month: string) => this.http.get<Expense[]>('/api/expenses', { params: { month } });
  salaries = () => this.http.get<Salary[]>('/api/salaries');
  recurrences = () => this.http.get<Recurrence[]>('/api/recurrences');
  createExpense = (body: ExpenseInput) => this.http.post<Expense>('/api/expenses', body);
  updateExpense = (id: number, body: ExpenseInput) => this.http.put<Expense>(`/api/expenses/${id}`, body);
  deleteExpense = (id: number) => this.http.delete<void>(`/api/expenses/${id}`);
  createRecurrence = (body: RecurrenceInput) => this.http.post<Recurrence>('/api/recurrences', body);
  updateRecurrence = (id: number, body: Omit<RecurrenceInput, 'startDate'>) => this.http.put<Recurrence>(`/api/recurrences/${id}`, body);
  cancelRecurrence = (id: number) => this.http.delete<void>(`/api/recurrences/${id}`);
  salary = (month: string, amount: number) => this.http.put<Salary>('/api/salary', { amount }, { params: { month } });
  color = (id: number, color: string) => this.http.patch<Category>(`/api/categories/${id}/color`, { color });
}

export function apiError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'Não foi possível concluir a operação. Tente novamente.';
  if (error.status === 0 || error.status === 502 || error.status === 503) return 'Não foi possível conectar ao servidor. Verifique se o aplicativo está em execução e tente atualizar.';
  const body = error.error;
  if (body && typeof body === 'object') {
    if (body.errors) return Object.values(body.errors).join(' ');
    if (typeof body.detail === 'string') return body.detail;
  }
  return 'Não foi possível concluir a operação. Tente novamente.';
}
