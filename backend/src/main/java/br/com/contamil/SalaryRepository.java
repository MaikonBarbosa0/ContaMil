package br.com.contamil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SalaryRepository extends JpaRepository<SalaryVersion, Long> {
    Optional<SalaryVersion> findByEffectiveMonth(LocalDate month);

    Optional<SalaryVersion> findFirstByEffectiveMonthLessThanEqualOrderByEffectiveMonthDesc(LocalDate month);

    List<SalaryVersion> findAllByOrderByEffectiveMonthDesc();
}
