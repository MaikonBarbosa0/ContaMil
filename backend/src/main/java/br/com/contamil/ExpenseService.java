package br.com.contamil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ExpenseService {
    private final ExpenseRepository expenses;
    private final RecurrenceService recurrences;
    private final SalaryService salaries;
    private final CategoryService categories;

    ExpenseService(ExpenseRepository expenses, RecurrenceService recurrences, SalaryService salaries,
                   CategoryService categories) {
        this.expenses = expenses;
        this.recurrences = recurrences;
        this.salaries = salaries;
        this.categories = categories;
    }

    @Transactional
    public List<ApiModels.ExpenseView> list(YearMonth month) {
        recurrences.generatePending();
        return monthExpenses(month).stream().map(ApiModels.ExpenseView::from).toList();
    }

    @Transactional
    public ApiModels.ExpenseView get(Long id) {
        recurrences.generatePending();
        return ApiModels.ExpenseView.from(find(id));
    }

    @Transactional
    public ApiModels.ExpenseView create(ApiModels.ExpenseInput input) {
        Category category = categories.forInput(input.categoryId());
        Expense expense = new Expense(input.description(), input.amount(), input.date(), category);
        return ApiModels.ExpenseView.from(expenses.save(expense));
    }

    @Transactional
    public ApiModels.ExpenseView update(Long id, ApiModels.ExpenseInput input) {
        Category category = categories.forInput(input.categoryId());
        recurrences.generatePending();
        Expense expense = find(id);
        expense.update(input, category);
        return ApiModels.ExpenseView.from(expense);
    }

    @Transactional
    public void delete(Long id) {
        recurrences.generatePending();
        Expense expense = find(id);
        // Conserva a identidade da ocorrência para que a recorrência não a recrie.
        expense.deleted = true;
    }

    @Transactional
    public ApiModels.Summary summary(YearMonth month) {
        recurrences.generatePending();
        List<Expense> monthlyExpenses = monthExpenses(month);
        BigDecimal total = new BigDecimal("0.00");
        Map<Long, BigDecimal> categoryTotals = new HashMap<>();
        Map<Long, Category> usedCategories = new HashMap<>();
        Map<Long, Map<LocalDate, BigDecimal>> categoryDailyTotals = new HashMap<>();
        Map<LocalDate, BigDecimal> daily = new TreeMap<>();
        for (Expense expense : monthlyExpenses) {
            total = total.add(expense.amount);
            Long categoryId = expense.category.getId();
            usedCategories.put(categoryId, expense.category);
            categoryTotals.merge(categoryId, expense.amount, BigDecimal::add);
            categoryDailyTotals.computeIfAbsent(categoryId, ignored -> new HashMap<>())
                    .merge(expense.date, expense.amount, BigDecimal::add);
            daily.merge(expense.date, expense.amount, BigDecimal::add);
        }
        List<ApiModels.CategoryTotal> byCategory = categoryTotals.entrySet().stream()
                .map(entry -> new ApiModels.CategoryTotal(ApiModels.CategoryView.from(usedCategories.get(entry.getKey())),
                        entry.getValue()))
                .sorted(Comparator.comparing(ApiModels.CategoryTotal::amount).reversed()
                        .thenComparing(item -> item.category().name()))
                .toList();
        List<ApiModels.DailyTotal> dailyExpenses = dailyPoints(month, daily);
        List<ApiModels.CategoryDailySeries> dailyByCategory = byCategory.stream()
                .map(item -> new ApiModels.CategoryDailySeries(item.category(),
                        dailyPoints(month, categoryDailyTotals.get(item.category().id()))))
                .toList();
        BigDecimal salary = salaries.forMonth(month).amount();
        return new ApiModels.Summary(month.toString(), salary, total, salary.subtract(total), byCategory,
                dailyExpenses, dailyByCategory);
    }

    private List<ApiModels.DailyTotal> dailyPoints(YearMonth month, Map<LocalDate, BigDecimal> amounts) {
        List<ApiModels.DailyTotal> points = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            points.add(new ApiModels.DailyTotal(date, amounts.getOrDefault(date, new BigDecimal("0.00"))));
        }
        return points;
    }

    private List<Expense> monthExpenses(YearMonth month) {
        return expenses.findByDeletedFalseAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
                month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private Expense find(Long id) {
        return expenses.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto não encontrado."));
    }
}
