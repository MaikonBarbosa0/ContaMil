package br.com.contamil;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RecurrenceService {
    private final RecurrenceRepository recurrences;
    private final ExpenseRepository expenses;
    private final Clock clock;
    private final CategoryService categories;

    RecurrenceService(RecurrenceRepository recurrences, ExpenseRepository expenses, Clock clock,
                      CategoryService categories) {
        this.recurrences = recurrences;
        this.expenses = expenses;
        this.clock = clock;
        this.categories = categories;
    }

    @Transactional
    public void generatePending() {
        LocalDate today = LocalDate.now(clock);
        for (Recurrence recurrence : recurrences.findActiveForUpdate()) {
            generateThrough(recurrence, today);
        }
    }

    @Transactional
    public List<ApiModels.RecurrenceView> list() {
        generatePending();
        return recurrences.findAllByOrderByIdDesc().stream().map(ApiModels.RecurrenceView::from).toList();
    }

    @Transactional
    public ApiModels.RecurrenceView get(Long id) {
        generatePending();
        return recurrences.findById(id).map(ApiModels.RecurrenceView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrência não encontrada."));
    }

    @Transactional
    public ApiModels.RecurrenceView create(ApiModels.RecurrenceInput input) {
        Category category = categories.forInput(input.categoryId());
        Recurrence recurrence = recurrences.saveAndFlush(new Recurrence(input, category));
        generateThrough(recurrence, LocalDate.now(clock));
        return ApiModels.RecurrenceView.from(recurrence);
    }

    @Transactional
    public ApiModels.RecurrenceView update(Long id, ApiModels.RecurrenceUpdate input) {
        Category category = categories.forInput(input.categoryId());
        Recurrence recurrence = findForUpdate(id);
        if (!recurrence.active) {
            throw new InvalidInputException("Esta recorrência foi cancelada. Crie uma nova para voltar a lançar gastos.");
        }
        // Primeiro registra os vencimentos antigos; a nova regra vale somente para os próximos.
        generateThrough(recurrence, LocalDate.now(clock));
        recurrence.update(input, category);
        return ApiModels.RecurrenceView.from(recurrence);
    }

    @Transactional
    public void cancel(Long id) {
        Recurrence recurrence = findForUpdate(id);
        if (recurrence.active) {
            generateThrough(recurrence, LocalDate.now(clock));
            recurrence.active = false;
        }
    }

    private Recurrence findForUpdate(Long id) {
        return recurrences.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrência não encontrada."));
    }

    private void generateThrough(Recurrence recurrence, LocalDate throughDate) {
        while (recurrence.active && !recurrence.nextDate().isAfter(throughDate)) {
            LocalDate occurrenceDate = recurrence.nextDate();
            // A chave de ocorrência é imutável, mesmo se o gasto for editado ou excluído.
            if (!expenses.existsByRecurrenceIdAndOccurrenceMonth(recurrence.id, recurrence.nextMonth)) {
                Expense expense = new Expense(recurrence.description, recurrence.amount, occurrenceDate,
                        recurrence.category);
                expense.recurrenceId = recurrence.id;
                expense.occurrenceMonth = recurrence.nextMonth;
                expenses.save(expense);
            }
            recurrence.advance();
        }
    }
}
