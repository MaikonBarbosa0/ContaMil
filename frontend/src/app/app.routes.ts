import { Routes } from '@angular/router';
import { SummaryPage, ExpensesPage, SalaryPage, CategoriesPage } from './pages';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'resumo' },
  { path: 'resumo', component: SummaryPage, title: 'Visão geral · CONTAMIL' },
  { path: 'gastos', component: ExpensesPage, title: 'Gastos · CONTAMIL' },
  { path: 'salario', component: SalaryPage, title: 'Salário · CONTAMIL' },
  { path: 'categorias', component: CategoriesPage, title: 'Categorias · CONTAMIL' },
  { path: '**', redirectTo: 'resumo' },
];
