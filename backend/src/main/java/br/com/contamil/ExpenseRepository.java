package br.com.contamil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @EntityGraph(attributePaths = "category")
    List<Expense> findByDeletedFalseAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
            LocalDate firstDay, LocalDate nextMonth);

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndDeletedFalse(Long id);

    boolean existsByRecurrenceIdAndOccurrenceMonth(Long recurrenceId, LocalDate occurrenceMonth);
}
