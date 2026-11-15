package br.com.contamil;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SalaryService {
    private final SalaryRepository salaries;

    SalaryService(SalaryRepository salaries) {
        this.salaries = salaries;
    }

    @Transactional(readOnly = true)
    public ApiModels.SalaryView forMonth(YearMonth month) {
        return salaries.findFirstByEffectiveMonthLessThanEqualOrderByEffectiveMonthDesc(month.atDay(1))
                .map(version -> new ApiModels.SalaryView(month.toString(),
                        YearMonth.from(version.effectiveMonth).toString(), version.amount))
                .orElseGet(() -> new ApiModels.SalaryView(month.toString(), null, new BigDecimal("0.00")));
    }

    @Transactional
    public ApiModels.SalaryView set(YearMonth month, BigDecimal amount) {
        SalaryVersion version = salaries.findByEffectiveMonth(month.atDay(1))
                .orElseGet(() -> new SalaryVersion(month.atDay(1), amount));
        version.amount = amount;
        salaries.save(version);
        return new ApiModels.SalaryView(month.toString(), month.toString(), amount);
    }

    @Transactional(readOnly = true)
    public List<ApiModels.SalaryView> history() {
        return salaries.findAllByOrderByEffectiveMonthDesc().stream()
                .map(version -> new ApiModels.SalaryView(YearMonth.from(version.effectiveMonth).toString(),
                        YearMonth.from(version.effectiveMonth).toString(), version.amount))
                .toList();
    }
}
