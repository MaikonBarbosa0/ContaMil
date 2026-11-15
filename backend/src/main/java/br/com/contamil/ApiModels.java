package br.com.contamil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

final class ApiModels {
    private ApiModels() {
    }

    record ExpenseInput(
            @NotBlank(message = "Informe a descrição.") @Size(max = 180, message = "Use até 180 caracteres na descrição.") String description,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "O gasto deve ser maior que zero.") @Digits(integer = 12, fraction = 2, message = "Informe um valor com até 12 dígitos inteiros e 2 casas decimais.") BigDecimal amount,
            @NotNull(message = "Informe a data.") LocalDate date,
            @NotNull(message = "Selecione uma categoria.") @Positive(message = "Selecione uma categoria válida.") Long categoryId) {
    }

    record RecurrenceInput(
            @NotBlank(message = "Informe a descrição.") @Size(max = 180, message = "Use até 180 caracteres na descrição.") String description,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "O gasto deve ser maior que zero.") @Digits(integer = 12, fraction = 2, message = "Informe um valor com até 12 dígitos inteiros e 2 casas decimais.") BigDecimal amount,
            @NotNull(message = "Informe a data inicial.") LocalDate startDate,
            @NotNull(message = "Selecione uma categoria.") @Positive(message = "Selecione uma categoria válida.") Long categoryId) {
    }

    record RecurrenceUpdate(
            @NotBlank(message = "Informe a descrição.") @Size(max = 180, message = "Use até 180 caracteres na descrição.") String description,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "O gasto deve ser maior que zero.") @Digits(integer = 12, fraction = 2, message = "Informe um valor com até 12 dígitos inteiros e 2 casas decimais.") BigDecimal amount,
            @NotNull(message = "Selecione uma categoria.") @Positive(message = "Selecione uma categoria válida.") Long categoryId) {
    }

    record CategoryColorInput(
            @NotBlank(message = "Informe a cor.")
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Informe uma cor no formato #RRGGBB.") String color) {
    }

    record CategoryView(Long id, String name, String color) {
        static CategoryView from(Category category) {
            return new CategoryView(category.getId(), category.getName(), category.getColor());
        }
    }

    record SalaryInput(
            @NotNull(message = "Informe o salário.") @DecimalMin(value = "0.00", message = "O salário não pode ser negativo.") @Digits(integer = 12, fraction = 2, message = "Informe um valor com até 12 dígitos inteiros e 2 casas decimais.") BigDecimal amount) {
    }

    record ExpenseView(Long id, String description, BigDecimal amount, LocalDate date, CategoryView category,
                       Long recurrenceId) {
        static ExpenseView from(Expense expense) {
            return new ExpenseView(expense.id, expense.description, expense.amount, expense.date,
                    CategoryView.from(expense.category), expense.recurrenceId);
        }
    }

    record RecurrenceView(Long id, String description, BigDecimal amount, LocalDate startDate,
                          CategoryView category, boolean active, LocalDate nextDate) {
        static RecurrenceView from(Recurrence recurrence) {
            return new RecurrenceView(recurrence.id, recurrence.description, recurrence.amount,
                    recurrence.startDate, CategoryView.from(recurrence.category), recurrence.active,
                    recurrence.active ? recurrence.nextDate() : null);
        }
    }

    record SalaryView(String month, String effectiveMonth, BigDecimal amount) {
    }

    record CategoryTotal(CategoryView category, BigDecimal amount) {
    }

    record DailyTotal(LocalDate date, BigDecimal amount) {
    }

    record CategoryDailySeries(CategoryView category, List<DailyTotal> points) {
    }

    record Summary(String month, BigDecimal salary, BigDecimal totalExpenses, BigDecimal balance,
                   List<CategoryTotal> byCategory, List<DailyTotal> dailyExpenses,
                   List<CategoryDailySeries> dailyByCategory) {
    }

    static YearMonth month(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}")) {
                throw new IllegalArgumentException();
            }
            YearMonth month = YearMonth.parse(value);
            if (month.getYear() < 1) {
                throw new IllegalArgumentException();
            }
            return month;
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            throw new InvalidInputException("Informe o mês no formato AAAA-MM, por exemplo 2026-11.");
        }
    }
}
