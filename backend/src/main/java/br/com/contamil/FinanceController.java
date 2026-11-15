package br.com.contamil;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class FinanceController {
    private final ExpenseService expenses;
    private final RecurrenceService recurrences;
    private final SalaryService salaries;
    private final JdbcTemplate database;
    private final CategoryService categories;

    FinanceController(ExpenseService expenses, RecurrenceService recurrences, SalaryService salaries,
                      JdbcTemplate database, CategoryService categories) {
        this.expenses = expenses;
        this.recurrences = recurrences;
        this.salaries = salaries;
        this.database = database;
        this.categories = categories;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        database.queryForObject("select 1", Integer.class);
        return Map.of("status", "UP");
    }

    @GetMapping("/categories")
    List<ApiModels.CategoryView> categories() {
        return categories.list();
    }

    @PatchMapping("/categories/{id}/color")
    ApiModels.CategoryView updateCategoryColor(@PathVariable Long id,
                                               @RequestBody @Valid ApiModels.CategoryColorInput input) {
        return categories.updateColor(id, input);
    }

    @GetMapping("/salary")
    ApiModels.SalaryView salary(@RequestParam String month) {
        return salaries.forMonth(ApiModels.month(month));
    }

    @PutMapping("/salary")
    ApiModels.SalaryView setSalary(@RequestParam String month, @RequestBody @Valid ApiModels.SalaryInput input) {
        return salaries.set(ApiModels.month(month), input.amount());
    }

    @GetMapping("/salaries")
    List<ApiModels.SalaryView> salaryHistory() {
        return salaries.history();
    }

    @GetMapping("/expenses")
    List<ApiModels.ExpenseView> listExpenses(@RequestParam String month) {
        return expenses.list(ApiModels.month(month));
    }

    @GetMapping("/expenses/{id}")
    ApiModels.ExpenseView getExpense(@PathVariable Long id) {
        return expenses.get(id);
    }

    @PostMapping("/expenses")
    ResponseEntity<ApiModels.ExpenseView> createExpense(@RequestBody @Valid ApiModels.ExpenseInput input) {
        ApiModels.ExpenseView created = expenses.create(input);
        return ResponseEntity.created(URI.create("/api/expenses/" + created.id())).body(created);
    }

    @PutMapping("/expenses/{id}")
    ApiModels.ExpenseView updateExpense(@PathVariable Long id, @RequestBody @Valid ApiModels.ExpenseInput input) {
        return expenses.update(id, input);
    }

    @DeleteMapping("/expenses/{id}")
    ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenses.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recurrences")
    List<ApiModels.RecurrenceView> listRecurrences() {
        return recurrences.list();
    }

    @GetMapping("/recurrences/{id}")
    ApiModels.RecurrenceView getRecurrence(@PathVariable Long id) {
        return recurrences.get(id);
    }

    @PostMapping("/recurrences")
    ResponseEntity<ApiModels.RecurrenceView> createRecurrence(@RequestBody @Valid ApiModels.RecurrenceInput input) {
        ApiModels.RecurrenceView created = recurrences.create(input);
        return ResponseEntity.created(URI.create("/api/recurrences/" + created.id())).body(created);
    }

    @PutMapping("/recurrences/{id}")
    ApiModels.RecurrenceView updateRecurrence(@PathVariable Long id,
                                              @RequestBody @Valid ApiModels.RecurrenceUpdate input) {
        return recurrences.update(id, input);
    }

    @DeleteMapping("/recurrences/{id}")
    ResponseEntity<Void> cancelRecurrence(@PathVariable Long id) {
        recurrences.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    ApiModels.Summary summary(@RequestParam String month) {
        return expenses.summary(ApiModels.month(month));
    }
}
