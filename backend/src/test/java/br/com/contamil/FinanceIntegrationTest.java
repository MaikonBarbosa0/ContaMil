package br.com.contamil;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contamil;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import(FinanceIntegrationTest.TimeConfiguration.class)
class FinanceIntegrationTest {
    @Autowired RecurrenceService recurrences;
    @Autowired ExpenseService expenseService;
    @Autowired SalaryService salaries;
    @Autowired ExpenseRepository expenses;
    @Autowired RecurrenceRepository recurrenceRepository;
    @Autowired SalaryRepository salaryRepository;
    @Autowired MutableClock clock;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;

    @BeforeEach
    void clearDatabase() {
        expenses.deleteAll();
        recurrenceRepository.deleteAll();
        salaryRepository.deleteAll();
        clock.setDate(LocalDate.of(2026, 3, 31));
    }

    @Test
    void preservesDay31AcrossShortMonthsAndDoesNotGenerateTwice() {
        recurrences.create(recurring("Aluguel", "100.00", "2026-01-31"));
        recurrences.generatePending();
        recurrences.generatePending();

        assertThat(expenses.findAll()).extracting(expense -> expense.date)
                .containsExactlyInAnyOrder(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 3, 31));
        assertThat(recurrences.list()).singleElement()
                .extracting(ApiModels.RecurrenceView::nextDate).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void usesFebruary29InLeapYearWithoutLosingOriginalDay() {
        clock.setDate(LocalDate.of(2024, 3, 31));
        recurrences.create(recurring("Seguro", "50.00", "2024-01-31"));

        assertThat(expenses.findAll()).extracting(expense -> expense.date)
                .containsExactlyInAnyOrder(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29),
                        LocalDate.of(2024, 3, 31));
    }

    @Test
    void deletingOrEditingAnOccurrenceDoesNotAllowItToBeGeneratedAgain() {
        ApiModels.RecurrenceView recurrence = recurrences.create(recurring("Seguro", "80.00", "2026-01-31"));
        Expense february = expenses.findAll().stream().filter(expense -> expense.date.getMonthValue() == 2)
                .findFirst().orElseThrow();
        expenseService.update(february.id, new ApiModels.ExpenseInput("Ajustado", new BigDecimal("70.00"),
                LocalDate.of(2026, 2, 27), categoryId("Carro")));
        expenseService.delete(february.id);

        // Simula uma reexecução desde um mês antigo: a identidade da ocorrência deve sobreviver.
        Recurrence stored = recurrenceRepository.findById(recurrence.id()).orElseThrow();
        stored.nextMonth = LocalDate.of(2026, 2, 1);
        recurrenceRepository.saveAndFlush(stored);
        recurrences.generatePending();

        assertThat(expenses.count()).isEqualTo(3);
        assertThat(expenseService.list(YearMonth.of(2026, 2))).isEmpty();
        assertThat(expenses.findById(february.id)).get().extracting(expense -> expense.deleted).isEqualTo(true);
    }

    @Test
    void catchesUpBeforeEditingAndAppliesNewAmountOnlyToFutureOccurrences() {
        clock.setDate(LocalDate.of(2026, 2, 15));
        ApiModels.RecurrenceView recurrence = recurrences.create(recurring("Internet", "100.00", "2026-01-31"));

        clock.setDate(LocalDate.of(2026, 4, 15));
        recurrences.update(recurrence.id(), new ApiModels.RecurrenceUpdate("Internet nova",
                new BigDecimal("150.00"), categoryId("Moradia")));
        clock.setDate(LocalDate.of(2026, 5, 1));
        recurrences.generatePending();

        List<Expense> generated = expenses.findAll();
        assertThat(generated).filteredOn(expense -> expense.date.isBefore(LocalDate.of(2026, 4, 1)))
                .hasSize(3).allSatisfy(expense -> {
                    assertThat(expense.amount).isEqualByComparingTo("100.00");
                    assertThat(expense.description).isEqualTo("Internet");
                });
        assertThat(generated).filteredOn(expense -> expense.date.equals(LocalDate.of(2026, 4, 30)))
                .singleElement().satisfies(expense -> {
                    assertThat(expense.amount).isEqualByComparingTo("150.00");
                    assertThat(expense.description).isEqualTo("Internet nova");
                });
    }

    @Test
    void cancellationCatchesUpAndKeepsHistoryWithoutFutureExpenses() {
        clock.setDate(LocalDate.of(2026, 1, 31));
        ApiModels.RecurrenceView recurrence = recurrences.create(recurring("Internet", "100.00", "2026-01-31"));
        clock.setDate(LocalDate.of(2026, 3, 10));
        recurrences.cancel(recurrence.id());
        recurrences.cancel(recurrence.id());
        clock.setDate(LocalDate.of(2026, 5, 1));
        recurrences.generatePending();

        assertThat(expenses.findAll()).extracting(expense -> expense.date)
                .containsExactlyInAnyOrder(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28));
        assertThat(recurrences.list()).singleElement().satisfies(item -> {
            assertThat(item.active()).isFalse();
            assertThat(item.nextDate()).isNull();
        });
    }

    @Test
    void futureRecurrenceDoesNotProduceAnExpenseBeforeStartDate() {
        recurrences.create(recurring("Assinatura", "29.90", "2026-04-05"));
        recurrences.generatePending();
        assertThat(expenses.findAll()).isEmpty();
        clock.setDate(LocalDate.of(2026, 4, 5));
        recurrences.generatePending();
        assertThat(expenses.findAll()).singleElement().extracting(expense -> expense.date)
                .isEqualTo(LocalDate.of(2026, 4, 5));
    }

    @Test
    void salaryChangesPreserveEarlierMonthsAndRespectLaterVersions() {
        salaries.set(YearMonth.of(2026, 1), new BigDecimal("3000.00"));
        salaries.set(YearMonth.of(2026, 4), new BigDecimal("3500.00"));
        salaries.set(YearMonth.of(2026, 7), new BigDecimal("4000.00"));
        salaries.set(YearMonth.of(2026, 4), new BigDecimal("3600.00"));

        assertThat(salaries.forMonth(YearMonth.of(2025, 12)).effectiveMonth()).isNull();
        assertThat(salaries.forMonth(YearMonth.of(2026, 3)).amount()).isEqualByComparingTo("3000.00");
        assertThat(salaries.forMonth(YearMonth.of(2026, 6)).amount()).isEqualByComparingTo("3600.00");
        assertThat(salaries.forMonth(YearMonth.of(2026, 8)).amount()).isEqualByComparingTo("4000.00");
        assertThat(salaries.history()).hasSize(3);
    }

    @Test
    void summaryUsesExactCentsAndExcludesDeletedExpensesAndOtherMonths() {
        salaries.set(YearMonth.of(2026, 3), new BigDecimal("1000.00"));
        expenseService.create(expense("Café", "0.10", "2026-03-01", "Mercado"));
        expenseService.create(expense("Pão", "0.20", "2026-03-01", "Mercado"));
        expenseService.create(expense("Ônibus", "5.00", "2026-03-02", "Carro"));
        expenseService.create(expense("Outro mês", "99.00", "2026-02-28", "Outros"));
        ApiModels.ExpenseView deleted = expenseService.create(expense("Excluído", "50.00", "2026-03-03", "Outros"));
        expenseService.delete(deleted.id());

        ApiModels.Summary result = expenseService.summary(YearMonth.of(2026, 3));
        assertThat(result.totalExpenses()).isEqualByComparingTo("5.30");
        assertThat(result.balance()).isEqualByComparingTo("994.70");
        assertThat(result.byCategory()).hasSize(2);
        assertThat(result.dailyExpenses()).hasSize(31);
        assertThat(result.dailyExpenses().getFirst().amount()).isEqualByComparingTo("0.30");
        assertThat(result.dailyByCategory()).hasSize(2).allSatisfy(series -> {
            assertThat(series.points()).hasSize(31);
            assertThat(series.category().id()).isNotNull();
            assertThat(series.category().name()).isNotBlank();
            assertThat(series.category().color()).matches("#[0-9a-fA-F]{6}");
        });
        ApiModels.CategoryDailySeries market = result.dailyByCategory().stream()
                .filter(series -> series.category().name().equals("Mercado")).findFirst().orElseThrow();
        assertThat(market.points().getFirst().amount()).isEqualByComparingTo("0.30");
        assertThat(market.points().get(1).amount()).isEqualByComparingTo("0.00");
        for (ApiModels.CategoryDailySeries series : result.dailyByCategory()) {
            BigDecimal seriesTotal = series.points().stream().map(ApiModels.DailyTotal::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ApiModels.CategoryTotal categoryTotal = result.byCategory().stream()
                    .filter(item -> item.category().id().equals(series.category().id())).findFirst().orElseThrow();
            assertThat(seriesTotal).isEqualByComparingTo(categoryTotal.amount());
            assertThat(series.category()).isEqualTo(categoryTotal.category());
        }
    }

    @Test
    void colorChangesAreReflectedInExistingExpensesRecurrencesAndChartSeries() {
        Long id = categoryId("Moradia");
        ApiModels.ExpenseView recorded = expenseService.create(expense("Compra antiga", "20.00", "2026-03-01", "Moradia"));
        ApiModels.RecurrenceView recurrence = recurrences.create(recurring("Internet", "100.00", "2026-01-31"));

        // Cada chamada abre outra transação, exercitando relações lazy sem sessão anterior.
        assertThat(recurrences.get(recurrence.id()).category().name()).isEqualTo("Moradia");
        categoryService.updateColor(id, new ApiModels.CategoryColorInput("#123ABC"));

        assertThat(expenseService.get(recorded.id()).category()).isEqualTo(new ApiModels.CategoryView(id, "Moradia", "#123ABC"));
        assertThat(recurrences.get(recurrence.id()).category().color()).isEqualTo("#123ABC");
        assertThat(recurrences.list()).singleElement().satisfies(item ->
                assertThat(item.category().color()).isEqualTo("#123ABC"));
        ApiModels.Summary summary = expenseService.summary(YearMonth.of(2026, 3));
        assertThat(summary.byCategory()).singleElement().satisfies(item -> {
            assertThat(item.category()).isEqualTo(new ApiModels.CategoryView(id, "Moradia", "#123ABC"));
            assertThat(item.amount()).isEqualByComparingTo("120.00");
        });
        assertThat(summary.dailyByCategory()).singleElement().satisfies(item ->
                assertThat(item.category().color()).isEqualTo("#123ABC"));
        assertThat(categories.findById(id)).get().extracting(Category::getName).isEqualTo("Moradia");
    }

    @Test
    void changingRecurrenceCategoryPreservesTheCategoryOfPreviousOccurrences() {
        clock.setDate(LocalDate.of(2026, 1, 31));
        ApiModels.RecurrenceView recurrence = recurrences.create(recurring("Internet", "100.00", "2026-01-31"));
        clock.setDate(LocalDate.of(2026, 2, 15));
        recurrences.update(recurrence.id(), new ApiModels.RecurrenceUpdate("Internet", new BigDecimal("100.00"),
                categoryId("Assinaturas")));
        clock.setDate(LocalDate.of(2026, 3, 1));

        assertThat(expenseService.list(YearMonth.of(2026, 1))).singleElement().satisfies(item ->
                assertThat(item.category().name()).isEqualTo("Moradia"));
        assertThat(expenseService.list(YearMonth.of(2026, 2))).singleElement().satisfies(item ->
                assertThat(item.category().name()).isEqualTo("Assinaturas"));
    }

    private ApiModels.RecurrenceInput recurring(String description, String amount, String date) {
        return new ApiModels.RecurrenceInput(description, new BigDecimal(amount), LocalDate.parse(date), categoryId("Moradia"));
    }

    private ApiModels.ExpenseInput expense(String description, String amount, String date, String category) {
        return new ApiModels.ExpenseInput(description, new BigDecimal(amount), LocalDate.parse(date), categoryId(category));
    }

    private Long categoryId(String name) {
        return categories.findByName(name).orElseThrow().id;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfiguration {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {
        private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
        private Instant value = LocalDate.of(2026, 3, 31).atTime(12, 0).atZone(ZONE).toInstant();

        void setDate(LocalDate date) {
            value = date.atTime(12, 0).atZone(ZONE).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(value, zone);
        }

        @Override
        public Instant instant() {
            return value;
        }
    }
}
