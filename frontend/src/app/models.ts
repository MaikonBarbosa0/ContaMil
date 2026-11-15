export interface Category { id: number; name: string; color: string; }
export interface Expense { id: number; description: string; amount: number; date: string; category: Category; recurrenceId: number | null; }
export interface Recurrence { id: number; description: string; amount: number; startDate: string; category: Category; active: boolean; nextDate: string | null; }
export interface Salary { month: string; effectiveMonth: string | null; amount: number; }
export interface DailyTotal { date: string; amount: number; }
export interface Summary {
  month: string; salary: number; totalExpenses: number; balance: number;
  byCategory: { category: Category; amount: number }[];
  dailyExpenses: DailyTotal[];
  dailyByCategory: { category: Category; points: DailyTotal[] }[];
}
export interface ExpenseInput { description: string; amount: number; date: string; categoryId: number; }
export interface RecurrenceInput { description: string; amount: number; startDate: string; categoryId: number; }
export type Editor = { kind: 'new' } | { kind: 'expense'; expense: Expense } | { kind: 'recurrence'; recurrence: Recurrence };
