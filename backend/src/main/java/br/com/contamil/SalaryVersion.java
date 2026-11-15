package br.com.contamil;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salary_versions")
class SalaryVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "effective_month", nullable = false, unique = true)
    LocalDate effectiveMonth;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal amount;

    protected SalaryVersion() {
    }

    SalaryVersion(LocalDate effectiveMonth, BigDecimal amount) {
        this.effectiveMonth = effectiveMonth;
        this.amount = amount;
    }
}
