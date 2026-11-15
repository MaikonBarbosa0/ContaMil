package br.com.contamil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

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
@Table(name = "recurrences")
class Recurrence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 180)
    String description;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "next_month", nullable = false)
    LocalDate nextMonth;

    @Column(nullable = false)
    boolean active = true;

    protected Recurrence() {
    }

    Recurrence(ApiModels.RecurrenceInput input, Category category) {
        description = input.description().trim();
        amount = input.amount();
        this.category = category;
        startDate = input.startDate();
        nextMonth = YearMonth.from(startDate).atDay(1);
    }

    LocalDate nextDate() {
        return occurrenceDate(YearMonth.from(nextMonth), startDate.getDayOfMonth());
    }

    static LocalDate occurrenceDate(YearMonth month, int originalDay) {
        return month.atDay(Math.min(originalDay, month.lengthOfMonth()));
    }

    void advance() {
        nextMonth = nextMonth.plusMonths(1);
    }

    void update(ApiModels.RecurrenceUpdate input, Category category) {
        description = input.description().trim();
        amount = input.amount();
        this.category = category;
    }
}
