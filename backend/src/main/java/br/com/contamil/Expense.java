package br.com.contamil;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 180)
    String description;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "recurrence_id")
    Long recurrenceId;

    @Column(name = "occurrence_month")
    LocalDate occurrenceMonth;

    @Column(nullable = false)
    boolean deleted;

    protected Expense() {
    }

    Expense(String description, BigDecimal amount, LocalDate date, Category category) {
        this.description = description.trim();
        this.amount = amount;
        this.date = date;
        this.category = category;
    }

    void update(ApiModels.ExpenseInput input, Category category) {
        description = input.description().trim();
        amount = input.amount();
        date = input.date();
        this.category = category;
    }
}
